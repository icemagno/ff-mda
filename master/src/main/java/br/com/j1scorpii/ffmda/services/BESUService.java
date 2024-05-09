package br.com.j1scorpii.ffmda.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class BESUService {
	private Logger logger = LoggerFactory.getLogger( BESUService.class );

	@Autowired private ImageManager imageManager;
	@Autowired private ContainerManager containerManager;
	@Autowired private LocalService localService;
	
	private final String COMPONENT_NAME = "besu";
	
	@Value("${ffmda.local.data.folder}")
	private String localDataFolder;	
	
	private String componentDataFolder;
	private String pluginsFolder;
	private String dataFolder;
	private String keysFolder;
	private String imageName;

	private String configFile;
	private String genesisFile;
	private String validatorsFile;
	private String keyFile;
	private String keyPubFile;
	private String staticNodesFile;
	private String permissionsFile;
	
	private JSONArray validatorsData;
	
	private RestTemplate rt;

	private JSONObject blockchainData = new JSONObject(); 
	
	@PostConstruct
	private void init() {
		// Set initial blockchain data
		blockchainData.put("blockNumber", -1);
		blockchainData.put("peers", new JSONArray() );
		
		this.rt = new RestTemplate();
		// Configure folders
		this.componentDataFolder = localDataFolder + "/" + COMPONENT_NAME;
		this.pluginsFolder = this.componentDataFolder + "/plugins";
		this.dataFolder = this.componentDataFolder + "/data";
		
		this.configFile = this.dataFolder + "/config.toml";
		this.validatorsFile = this.dataFolder + "/validators.json";
		this.genesisFile = this.dataFolder + "/genesis.json";
		this.keyFile = this.dataFolder + "/key";
		this.keyPubFile = this.dataFolder + "/key.pub";
		this.staticNodesFile = this.dataFolder + "/static-nodes.json";
		this.permissionsFile = this.dataFolder + "/permissions_config.toml";
		this.keysFolder = this.dataFolder + "/nodefiles/keys";
		
		logger.info("init " + this.componentDataFolder );
		new File( this.dataFolder ).mkdirs();
		
		loadValidatorsData();
		getConfig();
		copyDefaultData();
	}
	
	private String requestData( String endpoint, JSONObject payload ) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		RequestEntity<String> requestEntity = RequestEntity 
				.post( new URL( endpoint ).toURI() ) 
				.contentType( MediaType.APPLICATION_JSON ) 
				.body( payload.toString() ); 
		return rt.exchange(requestEntity, String.class ).getBody();		
	}

	public String getBlockchainData() {
		// blockchainData is global because this is a kind of asynchronous call.
		// The frontend will request the data and will not wait for all calls to the BESU node to finish.
		// So I'll respond ASAP with the global variable and fill it as I finish each call to the node.
		// I'll assume the node is an EVM implementation ( like BESU ) and use Ethereum RPC API.
		// To serve other flavors it will be much more complex
		int id = 0;
		JSONObject requestData = new JSONObject()
				.put("jsonrpc", "2.0")
				.put("id", id)
				.put("params", new JSONArray() );
		JSONObject res = null;
		BigInteger value = null;
		
		try {
			// Get block number
			requestData.put("method", "eth_blockNumber");
			res = new JSONObject ( this.requestData("http://besu:8545", requestData) );
			value = new BigInteger( res.getString("result").substring(2) , 16);
			blockchainData.put("blockNumber", value.toString() );
			
			// Get connected peers
			requestData.put("method", "admin_peers");
			res = new JSONObject ( this.requestData("http://besu:8545", requestData) );
			blockchainData.put("peers", res.getJSONArray("result") );
			
		} catch ( Exception e ) {  }
		
		
		return blockchainData.toString();
	}	
	
	// Start the container. Duh.
	public String startContainer() {
		JSONObject container = getContainer();
		if( container.has("State") ) {
			String state = container.getString("State");
			if( state.equals("running") ) return new JSONObject().put("result", "Already Running").toString();
			return new JSONObject().put("result", containerManager.startContainer( COMPONENT_NAME ) ).toString();
		}
		
		// We don't have any image yet. Pull it now
		if( this.imageName == null ) {
			this.pullImage();
			this.getConfig();
		}
		 
		JSONObject portBidings = new JSONObject();
		portBidings.put("30303", "30303/udp");
		portBidings.put("30303", "30303/tcp");
		portBidings.put("8545", "8545/tcp");

		JSONArray envs = new JSONArray();
		envs.put("BESU_OPTS=-Xmx4g");

		JSONArray volumes = new JSONArray();
		volumes.put("/etc/localtime:/etc/localtime:ro");
		volumes.put(  this.pluginsFolder + ":/besu/plugins");
		volumes.put(  this.dataFolder + ":/data");
		
		JSONObject containerDef = new JSONObject();
		containerDef.put("name", COMPONENT_NAME);
		containerDef.put("hostName", COMPONENT_NAME);
		containerDef.put("ports", portBidings );
		containerDef.put("image", this.imageName );
		containerDef.put("connectToNetwork", "ffmda");
		containerDef.put("restart", "always");
		containerDef.put("environments", envs);
		containerDef.put("volumes", volumes);
		containerDef.put("args", new JSONArray().put("/besu/bin/besu").put("--config-file=/data/config.toml") );
		
		String result = this.containerManager.create( containerDef );
		containerDef.put("result", new JSONObject( result ) );
		containerManager.startContainer(COMPONENT_NAME);
		return containerDef.toString();
	}
	
	// Get the image data
	public JSONObject imagePulled() {
		JSONObject result = new JSONObject();
		boolean exists = imageManager.exists(COMPONENT_NAME);
		result.put("exists", exists);
		if( exists ) {
			this.imageName = imageManager.getImageForComponent(COMPONENT_NAME);
			result.put("imageName", this.imageName );
		}
		return result;
	}
	
	private JSONObject getContainer() {
		JSONObject container = containerManager.getContainer( COMPONENT_NAME ); 
		return container;
	}

	public String pullImage() {
		return imageManager.pullImage(COMPONENT_NAME, true );
	}
	
	public String getConfig( ) {
		JSONObject localAgentConfig = localService.getMainConfig();
		// Use a object wrapper to send component configuration 
		// plus some relevant configuration to the UI.
		JSONObject generalConfig = new JSONObject();
		generalConfig.put("image", imagePulled() );
		generalConfig.put("container", getContainer() );
		// Plus the local node config ( I need this server's IP and host )
		generalConfig.put("localAgentConfig", localAgentConfig );
		generalConfig.put("validators", this.validatorsData );
		return generalConfig.toString();
	}

	// Allow to the user download the node files.
	// So they can be configured as the needs of the user and uploaded again.
	// Common files like Genesis will be propagated to the nodes so it will be no need
	// to user take care about configure nodes. I'll will automate from this point.
	public Resource downloadFile( String fileName, HttpServletResponse response ) {
    	Path path = null;
    	String actualFileName = null;
    	try {
	    	switch ( fileName ) {
	    		case "config":
	    			path = Paths.get( this.configFile );
	    			actualFileName = "config.toml";
	    			break;
	    		case "genesis":
	    			path = Paths.get( this.genesisFile );
	    			actualFileName = "genesis.json";
	    			break;
	    		case "key":
	    			path = Paths.get( this.keyFile );
	    			actualFileName = "key";
	    			break;
	    		case "keypub":
	    			path = Paths.get( this.keyPubFile );
	    			actualFileName = "key.pub";
	    			break;
	    		case "staticnodes":
	    			path = Paths.get( this.staticNodesFile );
	    			actualFileName = "static-nodes.json";
	    			break;
	    		case "permissions":
	    			path = Paths.get( this.permissionsFile );
	    			actualFileName = "permissions_config.toml";
	    			break;
	    		case "validatorpool":
	    			path = Paths.get( this.validatorsFile );
	    			actualFileName = "validators.json";
	    			break;
	    	}
	    	response.setHeader("Content-Disposition", "attachment; filename=" + actualFileName );
	    	ByteArrayResource resource = new ByteArrayResource( Files.readAllBytes( path ) );
		    return resource;
    	} catch ( Exception e ) {
    		e.printStackTrace();
    	}
    	return null;
	}

	// Take a node config file uploaded by user and put at the BESU data folder
	// They will be propagated to the other nodes later.
	public String receiveFile(MultipartFile file) {
		try {
			Path targetPath = Paths.get( this.dataFolder );
			Path targetFile = targetPath.resolve( file.getOriginalFilename()  );
			Files.copy( file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
			return "Ok";
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	private void loadValidatorsData() {
		try {
			this.validatorsData = new JSONArray( loadFile( this.validatorsFile ) );
			logger.info("We have " + this.validatorsData.length() + " validator keys" );
		} catch ( Exception e ) { this.validatorsData = new JSONArray();  }
	}
	
	private void saveValidatorsData() throws Exception {
		saveFile( this.validatorsFile, this.validatorsData.toString() );
	}
	
	// Will copy the blockchain default data to the data folder
	// The user must download from web interface, change as it needs and then upload again. 
	private void copyDefaultData() {

		// Do it just once
		// It will prevent override the files every time the container restarts
		if( new File( this.genesisFile ).exists() ) {
			logger.info("Genesis file already in place. Assuming that keys was created too");
			return;
		}
		logger.info("Initializing BESU files");
		// It is the first time. Do it.
		try {
			FileUtils.copyDirectory( new File("/besu-data"), new File( this.dataFolder ) );
			// Generate the Genesis file and 20 validator node keys
			// based on the 'bc_config.json' file 
			createValidatorNodes();
			getConfig();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
	// Run a BESU container just to create some node keys and addresses
	// as the genesis file to be used in our new blockchain.
	// The user can always edit these files from the web interface.
	// This must be done just once. I'll check the presence of the genesis file to
	// determine if I must do this. See 'copyDefaultData()'
	// This container will be removed.
	private void createValidatorNodes() {
		this.validatorsData = new JSONArray();
		logger.info("creating validators keys and Genesis file");
		String[] command = { 
			"/besu/bin/besu",
			"operator",
			"generate-blockchain-config",
			"--config-file=/data/bc_config.json",
			"--to=/data/nodefiles",
			"--private-key-file-name=key"
		};
		// Run a temp BESU container to create the Genesis file and the validators keys
		// TODO: Edit the chain ID, wallets and balances
		
		System.out.println("-------------------- BESUService --------------------");
		System.out.println(" What about allow some Genesis customization?        ");
		System.out.println("-----------------------------------------------------");
		
		this.containerManager.executeAndRemoveContainer( "besu_create_genesis", this.imageName, command, this.dataFolder, "/data" );
		File genesisFile = new File( this.dataFolder + "/nodefiles/genesis.json");
		
		logger.info("waiting for container 'besu_create_genesis' to generate genesis file and keys ... ");
		while( !genesisFile.exists() ) {
			// Wait to genesis.json be present
		}
		logger.info("Done. will copy genesis file to data folder.");
		try {
			FileUtils.copyFile( genesisFile , new File(this.genesisFile ) );
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// At this point we must have a genesis file and a folder with 20 validators keys.
		// Do NOT FORGET that all these validators addresses are in the genesis file as a node validator.
		// So... the first 20 nodes you create will become a validator with no need to register into BC.
		// In many common cases 5 validators are enough but I'll reserve a pool of 20 just in case...
		// If you decide to create the 21th node then no key pair will be send and BESU will
		// generate they making the node to not be a validator (it will not be in genesis)
		// Need to register by API (or let it go as is)

		// Here I'll read all key files generated and populate an array to serve as repository
		// So it will be easy to take keys to distribute to new nodes from memory instead read from disk
		try {
			File kf = new File( this.keysFolder );
			String[] listOfFolders = kf.list();
			for ( String address: listOfFolders ) {
				// Take the content of the files
			    Scanner s1 = new Scanner( new File( this.dataFolder + "/nodefiles/keys/" + address + "/key" ) );
			    Scanner s2 = new Scanner( new File( this.dataFolder + "/nodefiles/keys/" + address + "/key.pub" ) );
			    String privKey = s1.nextLine();
			    String pubKey = s2.nextLine();
				// Create an entry to store address, private and public keys for each validator
				JSONObject nd = new JSONObject()
			    		.put("address", address)
			    		.put("available", true)
			    		.put("privKey", privKey.trim() )
			    		.put("pubKey", pubKey.trim() )
			    		.put("usedByNode", JSONObject.NULL );
			    this.validatorsData.put(nd);
			    s1.close();
			    s2.close();
			}
			
			// Reserve the first key pair to this node
			logger.info("reserving keys to this node");
			// Since it is the first time I call this function, it will take the 
			// index zero (the first entry)
			int av = getNextAvailableValidator();
			// Yes I know.. I call the object every time to make sure the global array
			// will reflect the changes so I can save it to disk. Need to improve this later.
			String address = this.validatorsData.getJSONObject(av).getString("address");
			// Save keys to disk
			FileWriter w1 = new FileWriter( this.dataFolder + "/key" );
			FileWriter w2 = new FileWriter( this.dataFolder + "/key.pub" );
			w1.write( this.validatorsData.getJSONObject(av).getString("privKey") );
			w2.write( this.validatorsData.getJSONObject(av).getString("pubKey") );
			w1.close();
			w2.close();
			// Mark the entry as used by this node so no one can take it again
			this.validatorsData.getJSONObject(av).put("available", false);
			this.validatorsData.getJSONObject(av).put("usedByNode", "local");
			logger.info("this BESU node will use address " + address);
			// Update the validators repository to disk
			saveValidatorsData();
			// Remove the original genesis config file. No need to keep it
			new File(this.dataFolder + "/bc_config.json").delete();
		} catch ( Exception e ) { e.printStackTrace(); }		
		
	}
	
	// Every time you register a new node, I need to send a key pair to it.
	// So it will become a BESU validator (don't forget it was already registered into genesis file.
	// no need to register again)
	private int getNextAvailableValidator() {
		for( int x=0; x < this.validatorsData.length(); x++ ) {
			if ( this.validatorsData.getJSONObject(0).getBoolean("available") == true ) return x;
		}
		return -1;
	}

	// *******************************************************************************
	
	private void saveFile( String file, String data ) throws Exception {
		BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );
		writer.write( data );
		writer.close();			
	}
	
	private String loadFile( String file ) {
		try {
			byte[] encoded = Files.readAllBytes(Paths.get( file ));
			return new String(encoded, StandardCharsets.UTF_8 ) ;
		} catch ( Exception e ) {   }
		return null;
	}
	
	public void updateStaticNode( String enode, boolean remove ) {
		JSONArray staticNodes = new JSONArray();
		try { 
			// Copy enodes and make sure to delete proposed enode if exists
			JSONArray staticNodesTmp = new JSONArray( loadFile( this.staticNodesFile ) );
			for ( int x=0; x< staticNodesTmp.length(); x++ ) {
				if( !enode.equals(staticNodesTmp.get(x) ) ) staticNodes.put( enode );
			}
			// May I request this peer to delete that enode? 
			// Think about it later. It will removed when restart anyway
			
			// Save the updated list to 'static-nodes.json' file to make this node to remember
			// when restarts
			saveFile( this.staticNodesFile, staticNodes.toString() );
			
			// Just to remove. Quit now without adding
			if( remove ) return;
			
			// Add the new node to this node right now
			JSONObject requestData = new JSONObject()
					.put("jsonrpc", "2.0")
					.put("id", 99)
					.put("params", new JSONArray().put(enode) )
					.put("method", "admin_addPeer");
			this.requestData("http://besu:8545", requestData);
		} catch (Exception e) {	e.printStackTrace(); }
		/*		
			[
			    "enode://f6f0628abeced644e5549cc4fe8463202058271eef3b4ea0f4ddec898ea369744940eac0503e7f3f8f652919eef8dd5d94786370832c4ed295e21016a1f9f268@node-01:30303",
			    "enode://28577627e0047dd243b938aeac5e6997b28b7bdeda7a92eb7f002a3243448af746baed5669cf236d3dc371bc70c88f549142afd8078f0e2133a2b39183b2121f@node-02:30304",
			    "enode://57a77df902756ab20a69932a45435e0d53ee1e6b9b1f3e2e6d46f3d1862fe9b600c9e43cad199e7e4c6d8080314e470f2d50a35f01ce5b00b6d60cf4122473c6@node-03:30305",
			    "enode://8446da4d3605215ccb561c84966fdabdf14a6611c5953d40fe16d3141b5d394acbb4ce69d9a1b4ff034d2bf3f09cdef069e6c67e331f3da581550b1e13019bb2@node-04:30306",
			    "enode://ba4bcebb4b7b97e5260de0c1d43918e9c8b742eda1e51aa05d7d9b6f051e0c02852a97d5ad3ca641ab87f90d794fdfff537a5ec7f28b8e58ed06efd648492607@node-05:30307"
			]
		*/
	}
	
}

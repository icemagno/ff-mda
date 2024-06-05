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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import br.com.j1scorpii.ffmda.util.IFireFlyComponent;
import br.com.j1scorpii.ffmda.util.IObservable;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;

@Service
@Order( Ordered.LOWEST_PRECEDENCE )
public class BESUService implements IFireFlyComponent, IObservable  {
	private Logger logger = LoggerFactory.getLogger( BESUService.class );

	@Autowired private ImageManager imageManager;
	@Autowired private ContainerManager containerManager;
	@Autowired private LocalService localService;
	@Autowired ImageDownloaderService downloaderService;
	
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

		// Fire the image downloader...
		// The method 'notify()' will be triggered when done.
		// This must be done in synch because the start process must not block
		downloaderService.pull(COMPONENT_NAME, this);
	}
	
	public String getDataFolder() {
		return dataFolder;
	}
	
	public String getGenesisFile() {
		return genesisFile;
	}

	// This method will be called when ImageDownloader downloads the image
	@Override
	public synchronized void notify( String componentName ) {
		// Continue to configuration
		logger.info("notified pull done for " + componentName );
		copyDefaultData();
		getConfig();
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

	public String getBlockchainData( ) {
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

			// Get peer enode
			requestData.put("method", "net_enode");
			res = new JSONObject ( this.requestData("http://besu:8545", requestData) );
			
			// Change '127.0.0.1' from enode address to the master's local IP address
			JSONObject localAgentConfig = localService.getMainConfig();
			String localIpAddress = localAgentConfig.getString("ipAddress");
			String enode = res.getString("result").replace("127.0.0.1", localIpAddress );
			blockchainData.put("enode", enode );
			
		} catch ( Exception e ) {
			//
		}
		
		
		return blockchainData.toString();
	}	
	
	// Start the container. 
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
		//containerDef.put("restart", "always");
		containerDef.put("environments", envs);
		containerDef.put("volumes", volumes);
		containerDef.put("args", new JSONArray().put("/besu/bin/besu").put("--config-file=/data/config.toml") );
		
		String result = this.containerManager.create( containerDef );
		containerDef.put("result", new JSONObject( result ) );
		containerManager.startContainer(COMPONENT_NAME);
		return containerDef.toString();
	}
	
	private boolean imageExists() {
		return imageManager.exists(COMPONENT_NAME);
	}
	
	// Get the image data
	public JSONObject imagePulled() {
		JSONObject result = new JSONObject();
		boolean exists = imageExists();
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
		// This must be blocking (synch) because startContainer() must wait until the image
		// was downloaded before try to start it. This is why I'll not use "DownloaderService"
		return imageManager.pullImage(COMPONENT_NAME, true );
	}
	
	public String getConfig( ) {
		logger.info("loading config");
		loadValidatorsData();
		JSONObject localAgentConfig = localService.getMainConfig();
		// Use a object wrapper to send component configuration 
		// plus some relevant configuration to the UI.
		JSONObject generalConfig = new JSONObject();
		generalConfig.put("image", imagePulled() );
		generalConfig.put("container", getContainer() );
		// Plus the local node config ( I need this server's IP and host )
		generalConfig.put("localAgentConfig", localAgentConfig );
		generalConfig.put("validators", this.validatorsData );
		logger.info("config loaded.");
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

	// Load the validators key pool
	private void loadValidatorsData() {
		try {
			this.validatorsData = new JSONArray( loadFile( this.validatorsFile ) );
			logger.info("We have " + this.validatorsData.length() + " validator keys" );
		} catch ( Exception e ) { this.validatorsData = new JSONArray();  }
	}
	
	private void saveValidatorsData() throws Exception {
		logger.info("saving validadors storage repository with " + this.validatorsData.length() + " keys" );
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
	public void createValidatorNodes() {
		// We don't have any BESU image yet ... go away from here now.
		if( !imageExists() ) return;
		
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
		this.containerManager.executeAndRemoveContainer( "besu_create_genesis", this.imageName, command, this.dataFolder, "/data" );
		File genesisFile = new File( this.dataFolder + "/nodefiles/genesis.json");
		
		logger.info("waiting for container 'besu_create_genesis' to generate genesis file and keys ... ");
		while( !genesisFile.exists() ) {
			// Wait to genesis.json be present
		}
		logger.info("Done. will copy genesis file to data folder.");
		try {
			FileUtils.copyFile( genesisFile , new File( this.genesisFile ) );
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// At this point we must have a genesis file and a folder with a lot of validators keys.
		// Do NOT FORGET that all these validators addresses are in the genesis file as a node validator.
		// So... these first nodes you create will become a validator with no need to register into BC.
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
			    String privKey = s1.nextLine().trim();
			    String pubKey = s2.nextLine().trim();
				// Create an entry to store address, private and public keys for each validator
				JSONObject nd = new JSONObject()
			    		.put("address", address)
			    		.put("available", true)
			    		.put("privKey", privKey )
			    		.put("pubKey", pubKey )
			    		.put("usedByNode", JSONObject.NULL );
			    this.validatorsData.put(nd);
			    s1.close();
			    s2.close();
			}
			
			// Reserve a key pair to this node
			logger.info("reserving keys to this node");
			JSONObject vd = generateValidatorKeyPair( this.dataFolder, "local" );
					
			// Remove the original genesis config file. No need to keep it
			new File(this.dataFolder + "/bc_config.json").delete();
			
			// Remove the original nodefiles folder because we have this information
			// on the validators repository
			// FileUtils.deleteDirectory( new File( this.dataFolder + "/nodefiles" ) );			
			
			// Now we must add this node's ENODE address to the permissions file
			String localIpAddress = localService.getMainConfig().getString("ipAddress");
			String pubKey = vd.getString("pubKey");
			String enodeAddress = makeEnodeAddress( localIpAddress, pubKey );
			updatePermissionsFile( enodeAddress );
			
			// Put this enode addres into static_nodes file
			updateStaticNode( enodeAddress, false );
		} catch ( Exception e ) { e.printStackTrace(); }		
		
	}

	// Every time you register a new node, I need to send a key pair to it.
	// So it will become a BESU validator (don't forget it was already registered into genesis file.
	// no need to register again)
	private int getNextAvailableValidator( String nodeName ) {
		logger.info("searching for next available validator key...");
		for( int x=0; x < this.validatorsData.length(); x++ ) {
			try {
				String usedBy = this.validatorsData.getJSONObject(x).getString( "usedByNode" );
				logger.info("  > index " + x + " used by " + usedBy );
				// This is my own key pair ( someone asked to regenerate files ) 
				if ( usedBy.equals( nodeName ) ) {
					logger.info("  > it is my own key pair (" + nodeName + ")" );
					return x;
				}
			} catch (Exception e) {
				// This slot are available ( we have getString( "usedByNode" ) failure because it is null ) 
				logger.info("  > index " + x + " available " );
				return x;
			}
			
		}
		return -1;
	}
	
	public JSONObject generateValidatorKeyPair( String toFolder, String nodeName ) throws Exception{
		int av = getNextAvailableValidator( nodeName );
		// Yes I know.. I call the object 'this.validatorsData' every time to make sure the global array
		// will reflect the changes so I can save it to disk. Not sure if it will be called by reference 
		// Need to believe in Java ... 
		String address = this.validatorsData.getJSONObject(av).getString("address");
		// Save keys to disk
		FileWriter w1 = new FileWriter( toFolder + "/key" );
		FileWriter w2 = new FileWriter( toFolder + "/key.pub" );
		JSONObject vd = this.validatorsData.getJSONObject(av);
		w1.write( vd.getString("privKey") );
		w2.write( vd.getString("pubKey") );
		w1.close();
		w2.close();
		// Mark the entry as used by this node so no one can take it again
		this.validatorsData.getJSONObject(av).put("available", false);
		this.validatorsData.getJSONObject(av).put("usedByNode", nodeName);
		logger.info("BESU node '" + nodeName + "' will use address " + address + " of entry " + av );
		// Update the validators repository to disk
		saveValidatorsData();
		return vd;
	}

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
			// just to make sure I'll not add more than once
			JSONArray staticNodesTmp = new JSONArray( loadFile( this.staticNodesFile ) );
			for ( int x=0; x< staticNodesTmp.length(); x++ ) {
				if( !enode.equals(staticNodesTmp.get(x) ) ) staticNodes.put( staticNodesTmp.get(x) );
			}
			
			if( !remove ) {
				// Put (back / new) proposed enode into static nodes file.
				staticNodes.put( enode );
				
				JSONObject requestData = new JSONObject()
						.put("jsonrpc", "2.0")
						.put("id", 99)
						.put("params", new JSONArray().put(enode) )
						.put("method", "admin_addPeer");
				// Add the new node to this node right now
				// Try..catch because the node may not exist yet
				try { this.requestData("http://besu:8545", requestData); } catch ( Exception e ) {}
			
			}
			
			// Save the updated list to 'static-nodes.json' file to make this node to remember
			// when restarts
			saveFile( this.staticNodesFile, staticNodes.toString() );
			
		} catch (Exception e) {	e.printStackTrace(); }
	}
	
	public String makeEnodeAddress(String ipAddress, String pubKey) {
		return "enode://" + pubKey.substring(2) + "@" + ipAddress + ":30303";
	}
	
	public void updatePermissionsFile( String enode ) throws Exception {
		// Load current file from local
		String pf = loadFile( this.permissionsFile );
		System.out.println( pf );
		// Already here?
		if( pf.contains(enode) ) return;
		
		// Convert to JSONArray
		String enArr = pf.replace("nodes-allowlist=", "");
		
		System.out.println( enArr );
		
		JSONArray enodeList = new JSONArray( enArr );
		// Put the new one
		enodeList.put(enode);
		
		System.out.println( enodeList.toString(5) );
		
		// Save
		FileWriter fw = new FileWriter( this.permissionsFile );
		fw.write( "nodes-allowlist=" + enodeList.toString() );
		fw.close();
		logger.info("ENODE created as " + enode );
	}
	
	@Override
	public String getComponentDataFolder() {
		return this.componentDataFolder;
	}

	
}

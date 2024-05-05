package br.com.j1scorpii.ffmda.services;

import java.io.File;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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
	private String keyFile;
	private String keyPubFile;
	private String staticNodesFile;
	private String permissionsFile;
	
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
		this.genesisFile = this.dataFolder + "/genesis.json";
		this.keyFile = this.dataFolder + "/key";
		this.keyPubFile = this.dataFolder + "/key.pub";
		this.staticNodesFile = this.dataFolder + "/static-nodes.json";
		this.permissionsFile = this.dataFolder + "/permissions_config.toml";
		this.keysFolder = this.dataFolder + "/nodefiles/keys";
		
		logger.info("init " + this.componentDataFolder );
		new File( this.dataFolder ).mkdirs();
		
		getConfig();
		copyDefaultData();
		createValidatorNodes();
		
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
			
		} catch ( Exception e ) { e.printStackTrace(); }
		
		
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
		JSONObject localAgentConfig = localService.getAgentConfig();
		// Use a object wrapper to send component configuration 
		// plus some relevant configuration to the UI.
		JSONObject generalConfig = new JSONObject();
		generalConfig.put("image", imagePulled() );
		generalConfig.put("container", getContainer() );
		// Plus the local node config ( I need this server's IP and host )
		generalConfig.put("localAgentConfig", localAgentConfig );
		
		
		try {
			loadNodeKeysToConfig();
		} catch ( Exception e ) {}
		
		
		return generalConfig.toString(5);
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


	
	// *********************************************
	
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
			// Generate the Genesis file and 10 validator node keys
			// based on the 'bc_config.json' file 
			createValidatorNodes();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
	private void loadNodeKeysToConfig() {
		File f = new File( this.keysFolder );
		String[] listOfFolders = f.list();
		for ( String address: listOfFolders ) {           
		    System.out.println( " > " + address ); 
		}		
	}
	
	private void createValidatorNodes() {
		logger.info("creating validators keys and Genesis file");
		String[] command = { 
			"/besu/bin/besu",
			"operator",
			"generate-blockchain-config",
			"--config-file=/data/bc_config.json",
			"--to=/data/nodefiles",
			"--private-key-file-name=key"
		};
		// Run a temp BESU container to create the Genesis file and the validators
		// TODO: Edit the wallets and balances
		this.containerManager.executeAndRemoveContainer( this.imageName, command, this.dataFolder, "/data" );
		try {
			FileUtils.copyFile( new File(this.dataFolder + "/nodefiles/genesis.json"), new File(this.genesisFile ) );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

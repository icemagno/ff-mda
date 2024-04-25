package br.com.j1scorpii.ffmda.services;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

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
	private String imageName;

	private String configFile;
	private String genesisFile;
	private String keyFile;
	private String keyPubFile;
	private String staticNodesFile;
	private String permissionsFile;
	
	@PostConstruct
	private void init() {
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
		
		logger.info("init " + this.componentDataFolder );
		new File( this.dataFolder ).mkdirs();
		copyDefaultData();
	}
	
	private void copyDefaultData() {
		try {
			FileUtils.copyDirectory( new File("/besu-data"), new File( this.dataFolder ) );
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
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
		return generalConfig.toString(5);
	}

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
	
}

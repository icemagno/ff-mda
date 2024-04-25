package br.com.j1scorpii.ffmda.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

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
	private JSONObject config;

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
	
	private void loadConfig() throws Exception {
		String content = readFile( this.configFile , StandardCharsets.UTF_8);
		this.config = new JSONObject(content);
	}
	
	private void saveConfig() throws Exception {
		BufferedWriter writer = new BufferedWriter( new FileWriter( this.configFile ) );
		writer.write( this.config.toString() );
		writer.close();			
	}

	private String readFile(String path, Charset encoding)  throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
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
		try { loadConfig(); } catch (Exception e) {	}
		JSONObject localAgentConfig = localService.getAgentConfig();
		// Use a object wrapper to send component configuration 
		// plus some relevant configuration to the UI.
		JSONObject generalConfig = new JSONObject();
		generalConfig.put("image", imagePulled() );
		generalConfig.put("container", getContainer() );
		// Plus the local node config ( I need this server's IP and host )
		generalConfig.put("localAgentConfig", localAgentConfig );
		generalConfig.put("nodeConfig", this.config );
		return generalConfig.toString(5);
	}
	
}

package br.com.j1scorpii.ffmda.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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
	private String stagingFolder;
	private String dataFolder;
	private String imageName;
	private String configFile;
	private JSONObject config;
	
	@PostConstruct
	private void init() {
		// Configure folders
		this.componentDataFolder = localDataFolder + "/" + COMPONENT_NAME;
		this.stagingFolder = this.componentDataFolder + "/staging";
		this.dataFolder = this.componentDataFolder + "/data";
		this.configFile = this.dataFolder + "/config";
		logger.info("init " + this.componentDataFolder );
		new File( this.dataFolder ).mkdirs();
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
		portBidings.put("36209", "4001/udp");
		portBidings.put("36209", "4001/tcp");
		portBidings.put("36206", "5001/tcp");
		portBidings.put("36207", "8080/tcp");
		
		JSONArray envs = new JSONArray();
		// envs.put("IPFS_LOGGING=DEBUG");
		// envs.put("IPFS_PROFILE=server");
		// envs.put("LIBP2P_FORCE_PNET='1'");
		// envs.put("IPFS_SWARM_KEY_FILE=/ipfs/swarm.key");

		JSONArray volumes = new JSONArray();
		volumes.put("/etc/localtime:/etc/localtime:ro");
		volumes.put(  this.stagingFolder + ":/export");
		volumes.put(  this.dataFolder + ":/data/ipfs");
		
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

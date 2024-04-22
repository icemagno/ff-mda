package br.com.j1scorpii.ffmda.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
public class IPFSService {
	private Logger logger = LoggerFactory.getLogger( IPFSService.class );

	@Autowired private ImageManager imageManager;
	@Autowired private ContainerManager containerManager;
	@Autowired private LocalService localService;
	
	private final String COMPONENT_NAME = "ipfs";
	
	@Value("${ffmda.local.data.folder}")
	private String localDataFolder;	
	
	private String componentDataFolder;
	private String swarmKeyFile;
	private String stagingFolder;
	private String dataFolder;
	private String imageName;
	private String configFile;
	
	@PostConstruct
	private void init() {
		this.componentDataFolder = localDataFolder + "/" + COMPONENT_NAME;
		this.swarmKeyFile = this.componentDataFolder + "/swarm.key";
		this.stagingFolder = this.componentDataFolder + "/staging";
		this.dataFolder = this.componentDataFolder + "/data";
		this.configFile = this.dataFolder + "/config";
		logger.info("init " + this.componentDataFolder );
		new File( this.componentDataFolder ).mkdirs();
		if( ! new File( this.swarmKeyFile ).exists() ) this.createSwarmKey();
	}
	
	private String getSwarmKey() throws Exception {
		byte[] encoded = Files.readAllBytes( Paths.get( this.swarmKeyFile ) );
		return new String(encoded, StandardCharsets.UTF_8 );
	}
	
	private void createSwarmKey() {
		try {
			BufferedWriter writer = new BufferedWriter( new FileWriter( this.swarmKeyFile ) );
			writer.write( "/key/swarm/psk/1.0.0/" );
			writer.newLine();
			writer.write( "/base16/" );
			writer.newLine();
			writer.write( "9894e894901eb5ff61fcc9fb219700ee08d6bb4804b6277256f003ed6366a3e0" );
			writer.newLine();
			writer.close();
		} catch (Exception e) {
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
		
		JSONObject portBidings = new JSONObject();
		portBidings.put("36209", "4001/udp");
		portBidings.put("36209", "4001/tcp");
		portBidings.put("36206", "5001/tcp");
		portBidings.put("36207", "8080/tcp");
		
		JSONArray envs = new JSONArray();
		envs.put("IPFS_PROFILE=server");
		envs.put("LIBP2P_FORCE_PNET='1'");
		envs.put("IPFS_SWARM_KEY_FILE=/ipfs/swarm.key");

		JSONArray volumes = new JSONArray();
		volumes.put("/etc/localtime:/etc/localtime:ro");
		volumes.put(  this.stagingFolder + ":/export");
		volumes.put(  this.dataFolder + ":/data/ipfs");
		volumes.put(  this.swarmKeyFile + ":/ipfs/swarm.key");
		
		JSONObject containerDef = new JSONObject();
		containerDef.put("name", COMPONENT_NAME);
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
	

}

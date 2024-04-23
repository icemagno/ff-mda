package br.com.j1scorpii.ffmda.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

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
	private JSONObject identity = new JSONObject();
	private JSONObject config;
	
	@PostConstruct
	private void init() {
		this.componentDataFolder = localDataFolder + "/" + COMPONENT_NAME;
		this.stagingFolder = this.componentDataFolder + "/staging";
		this.dataFolder = this.componentDataFolder + "/data";
		this.configFile = this.dataFolder + "/config";
		this.swarmKeyFile = this.dataFolder + "/swarm.key";
		logger.info("init " + this.componentDataFolder );
		new File( this.dataFolder ).mkdirs();
		if( ! new File( this.swarmKeyFile ).exists() ) this.createSwarmKey();
	}
	
	public String getSwarmKey() throws Exception {
		byte[] encoded = Files.readAllBytes( Paths.get( this.swarmKeyFile ) );
		return new String(encoded, StandardCharsets.UTF_8 );
	}
	
	private void createSwarmKey() {
		String swarmKey = UUID.randomUUID().toString() + UUID.randomUUID().toString();
		swarmKey = swarmKey.replaceAll("-", "").toLowerCase();
		try {
			BufferedWriter writer = new BufferedWriter( new FileWriter( this.swarmKeyFile ) );
			writer.write( "/key/swarm/psk/1.0.0/" );
			writer.newLine();
			writer.write( "/base16/" );
			writer.newLine();
			writer.write( swarmKey );
			writer.newLine();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private JSONObject getApiConfig() {
		return new JSONObject()
			.put("HTTPHeaders", new JSONObject()
				.put("Access-Control-Allow-Methods", new JSONArray()
						.put("GET")
						.put("PUT")
						.put("POST") )
				.put("Access-Control-Allow-Origin", new JSONArray()
						.put("*") )
		);
	}

	// We need to change some stuff from config file
	// to make the this IPFS network private.
	// I don't want to execute commands so I will create, start, stop, update config and start again.
	private void updateConfig() throws Exception {
		loadConfig();
		this.config.put("API", getApiConfig() );
		this.config.getJSONObject("Addresses").put("NoAnnounce", new JSONArray() );
		this.config.put("Bootstrap", JSONObject.NULL );
		this.identity = config.getJSONObject("Identity");
		
		System.out.println( this.identity.toString(5) );
		
		this.config.getJSONObject("Swarm").put("AddrFilters", JSONObject.NULL );
		this.config.getJSONObject("Routing").put("AcceleratedDHTClient", true);
		this.config.getJSONObject("Routing").put("Type", "dht");
		saveConfig();
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
		// envs.put("IPFS_PROFILE=server");
		// envs.put("LIBP2P_FORCE_PNET='1'");
		// envs.put("IPFS_SWARM_KEY_FILE=/ipfs/swarm.key");

		JSONArray volumes = new JSONArray();
		volumes.put("/etc/localtime:/etc/localtime:ro");
		volumes.put(  this.stagingFolder + ":/export");
		volumes.put(  this.dataFolder + ":/data/ipfs");
		
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
		
		try { 
			Thread.sleep( 5 * 1000 );
			containerManager.stopContainer(COMPONENT_NAME);			
			this.updateConfig(); 
			Thread.sleep( 5 * 1000 );
			containerManager.startContainer(COMPONENT_NAME);			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
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
	
	public String addPeer( String peerIp, String peerPort, String peerId ) {
		// docker exec ipfs ipfs cat QmT78zSuBmuS4z925WZfrqQ1qHaJ56DQaTfyMUF7F8ff5o
		// docker exec ipfs ipfs swarm connect /ip4/192.168.0.206/tcp/36209/ipfs/12D3KooWPmZUYKmhek1WnqyMWuWFMMncxLAirt6gTyf5DABfcFsn
		// docker exec ipfs ipfs swarm connect /ip4/172.22.1.49/tcp/10209/ipfs/12D3KooWN9Ksf9jmDuww7HKutNAknyVYnrvowPqYFZnEY8Mc5Gtp
		// docker exec ipfs ipfs ping 12D3KooWN9Ksf9jmDuww7HKutNAknyVYnrvowPqYFZnEY8Mc5Gtp	
		String[] command = {
				"ipfs", 
				"swarm", 
				"connect", 
				"/ip4/"+peerIp+"/tcp/"+peerPort+"/ipfs/" + peerId };
		return this.containerManager.exec("ipfs", command);
	}
	

}

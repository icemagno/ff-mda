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
public class DataExchangeService {
	private Logger logger = LoggerFactory.getLogger( DataExchangeService.class );

	@Autowired private ImageManager imageManager;
	@Autowired private ContainerManager containerManager;
	@Autowired private LocalService localService;
	
	private final String COMPONENT_NAME = "dataexchange";
	
	@Value("${ffmda.local.data.folder}")
	private String localDataFolder;	
	
	private String componentDataFolder;
	private String peersFolder;
	private String configFile;
	private String imageName;
	
	private JSONObject componentConfig;
	
	@PostConstruct
	private void init() {
		this.componentDataFolder  = localDataFolder + "/" + COMPONENT_NAME;
		this.peersFolder = this.componentDataFolder + "/peer-certs";
		this.configFile = this.componentDataFolder + "/config.json";
		new File( this.peersFolder ).mkdirs();
		loadConfig();
	}

	
	public boolean certAndKeysExists() {
		String hostName = this.localService.getAgentConfig().getString("hostName");
		String certificateFile = this.componentDataFolder + "/" + hostName + ".cer";
		String pemCer = this.componentDataFolder + "/cert.pem";
		String pemKey = this.componentDataFolder + "/key.pem";
		boolean result = ( 
				new File( certificateFile ).exists() &&
				new File( pemCer ).exists() &&
				new File( pemKey ).exists() );
		return result;
	}
	

	// We have the CA created, Org name and Node name.
	// I think we can create the DataExchange key pair and sign the certificate with the CA.
	private void createCertificateAndKeys() {
		boolean stackIsLocked = localService.getAgentConfig().getJSONObject("stackStatus").getBoolean("locked");
		String hostName = this.localService.getAgentConfig().getString("hostName");
		boolean stackCertsWasCreated = this.localService.getPkiManager().caWasCreated();
		if( stackIsLocked && stackCertsWasCreated && !certAndKeysExists() ) {
			this.localService.getPkiManager().createAndSignKeysAndCert( hostName , this.componentDataFolder );
		}
	}
	
	public String startContainer() {
		
		JSONObject portBidings = new JSONObject();
		portBidings.put("10205", "3000");
		portBidings.put("10204", "3001");
		
		JSONArray envs = new JSONArray();
		
		
		JSONArray volumes = new JSONArray();
		volumes.put("/etc/localtime:/etc/localtime:ro");
		volumes.put(  this.componentDataFolder + ":/data");
		
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
	
	
	public JSONObject getContainer() {
		JSONObject container = containerManager.getContainer( COMPONENT_NAME ); 
		return container;
	}


	public String pullImage() {
		return imageManager.pullImage(COMPONENT_NAME, true );
	}
	
	public String getConfig() {
		// If we don't have keys yet ...
		createCertificateAndKeys();
		// Refresh configuration variable
		loadConfig();
		// Use a object wrapper to send component configuration 
		// plus some relevant configuration to the UI.
		JSONObject generalConfig = new JSONObject();
		generalConfig.put("componentConfig", this.componentConfig);
		generalConfig.put("certAndKeysExists", certAndKeysExists() );
		generalConfig.put("image", imagePulled() );
		generalConfig.put("container", getContainer() );
		return generalConfig.toString(5);
	}
	
	private void loadConfig() {
		try {
			if( new File( this.configFile ).exists() ) {
				String content = readConfig( );
				this.componentConfig = new JSONObject(content);
			} else {
				logger.info("Configuration file not found. Will create one.");
				this.componentConfig = new JSONObject();
				JSONArray peers = new JSONArray();
				
				this.componentConfig
				.put("api", new JSONObject().put("hostname", "0.0.0.0").put("port", 3000)  )
				.put("p2p", new JSONObject().put("hostname", "0.0.0.0").put("port", 3001)  )
				.put("peers", peers);
				
				saveConfig();
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	
	private void saveConfig() throws Exception {
		BufferedWriter writer = new BufferedWriter( new FileWriter( this.configFile ) );
		writer.write( this.componentConfig.toString(5) );
		writer.close();			
	}		
	
	private String readConfig()  throws Exception {
		byte[] encoded = Files.readAllBytes( Paths.get( this.configFile ) );
		return new String(encoded, StandardCharsets.UTF_8 );
	}
	
	
}

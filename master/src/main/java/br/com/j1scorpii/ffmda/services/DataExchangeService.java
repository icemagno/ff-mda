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
	
	private JSONObject componentConfig;
	
	@PostConstruct
	private void init() {
		this.componentDataFolder  = localDataFolder + "/" + COMPONENT_NAME;
		this.peersFolder = this.componentDataFolder + "/peer-certs";
		this.configFile = this.componentDataFolder + "/config.json";
		new File( this.peersFolder ).mkdirs();
		loadConfig();
	}
	
	public void startContainer() {
		boolean stackIsLocked = localService.getAgentConfig().getJSONObject("stackStatus").getBoolean("locked");
		boolean stackCertsWasCreated = this.localService.getPkiManager().caWasCreated();
		if( stackIsLocked && stackCertsWasCreated  ) {
			// We have the CA created, Org name and Node name.
			// I think we can create the DataExchange keypair and sign the certificate with the CA.
			
		}
	}
	
	public String imagePulled() {
		JSONObject result = new JSONObject();
		boolean exists = imageManager.exists(COMPONENT_NAME);
		result.put("exists", exists);
		if( exists ) {
			result.put("imageName", imageManager.getImageForComponent(COMPONENT_NAME) );
		}
		return result.toString();
	}
	
	
	public String getContainer() {
		JSONObject container = containerManager.getContainer( COMPONENT_NAME ); 
		return container.toString();
	}


	public String pullImage() {
		return imageManager.pullImage(COMPONENT_NAME, true );
	}
	
	public String getConfig() {
		loadConfig();
		return this.componentConfig.toString(5);
	}
	
	private void loadConfig() {
		try {
			if( new File( this.configFile ).exists() ) {
				logger.info("Configuration file found.");
				String content = readConfig( );
				this.componentConfig = new JSONObject(content);
			} else {
				logger.info("Configuration file not found. Will create one.");
				this.componentConfig = new JSONObject();
				JSONArray peers = new JSONArray();
				
				this.componentConfig
				.put("api", new JSONObject().put("hostname", "0.0.0.0").put("port", "3000")  )
				.put("p2p", new JSONObject().put("hostname", "0.0.0.0").put("port", "3001")  )
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

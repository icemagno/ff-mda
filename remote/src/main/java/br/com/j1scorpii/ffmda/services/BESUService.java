package br.com.j1scorpii.ffmda.services;

import java.io.File;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import br.com.j1scorpii.ffmda.util.IFireFlyComponent;
import jakarta.annotation.PostConstruct;

@Service
public class BESUService implements IFireFlyComponent {
	private Logger logger = LoggerFactory.getLogger( BESUService.class );
	private RestTemplate rt;
	private final String COMPONENT_NAME = "besu";
	private String imageName;
	
	@Autowired private ImageManager imageManager;
	@Autowired private ContainerManager containerManager;	
	
	@Value("${ffmda.data.folder}")
	private String localDataFolder;
	
	private String componentDataFolder;
	private String pluginsFolder;
	private String dataFolder;
	
	@PostConstruct
	private void init() {
		this.componentDataFolder = localDataFolder + "/" + COMPONENT_NAME;
		this.pluginsFolder = this.componentDataFolder + "/plugins";		
		new File( this.componentDataFolder ).mkdirs();
		new File( this.pluginsFolder ).mkdirs();
		this.rt = new RestTemplate();
		logger.info("init");
		this.getConfig();
	}
	
	public JSONObject getConfig( ) {
		JSONObject generalConfig = new JSONObject();
		generalConfig.put("image", imagePulled() );
		generalConfig.put("container", getContainer() );
		generalConfig.put("enode", getEnode() );
		return generalConfig;
	}
	
	public JSONObject deploy( String imageName, CommService commService, String callbackChannel ) {
		// Check if we already have all files needed by the BESU
		// to start...
		// if( no files here) commandError() ... 
		System.out.println("Must deploy a BESU node.... " + imageName );
		imageManager.addToManifest( "besu", imageName );
		return startContainer( callbackChannel );
	}
	
	private JSONObject getContainer() {
		JSONObject container = containerManager.getContainer( COMPONENT_NAME ); 
		return container;
	}	
	
	public String pullImage( String callbackChannel ) {
		return imageManager.pullImage(COMPONENT_NAME, true, callbackChannel );
	}
	
	// Start the container. 
	public JSONObject startContainer(String callbackChannel) {
		JSONObject container = getContainer();
		if( container.has("State") ) {
			String state = container.getString("State");
			if( state.equals("running") ) return new JSONObject().put("result", "Already Running");
			return new JSONObject().put("result", containerManager.startContainer( COMPONENT_NAME ) );
		}
		
		// We don't have any image yet. Pull it now
		if( this.imageName == null ) {
			this.pullImage(callbackChannel);
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
		return containerDef;
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
	
	public JSONObject getEnode( ) {
		JSONObject res = new JSONObject ();
		if( !imagePulled().getBoolean("exists") ) return res;
		try {
			JSONObject requestData = new JSONObject()
					.put("jsonrpc", "2.0")
					.put("id", 99)
					.put("params", new JSONArray() )
					.put("method", "net_enode");
			res = new JSONObject ( this.requestData( "http://besu:8545", requestData) );
		} catch (Exception e) {  }
		return res; 
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



	@Override
	public String getComponentDataFolder() {
		return this.componentDataFolder;
	}
	
	
}

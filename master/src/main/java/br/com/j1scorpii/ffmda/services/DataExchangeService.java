package br.com.j1scorpii.ffmda.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import br.com.j1scorpii.ffmda.util.DXWebSocketHandler;
import jakarta.annotation.PostConstruct;

@Service
public class DataExchangeService {
	private Logger logger = LoggerFactory.getLogger( DataExchangeService.class );

	@Autowired private ImageManager imageManager;
	@Autowired private ContainerManager containerManager;
	@Autowired private LocalService localService;
	@Autowired private SimpMessagingTemplate messagingTemplate;

	
	private final String COMPONENT_NAME = "dataexchange";
	
	@Value("${ffmda.local.data.folder}")
	private String localDataFolder;	
	
	private String componentDataFolder;
	private String peersFolder;
	private String configFile;
	private String imageName;
	private String pemCer;
	private String pemKey;
	
	private JSONObject componentConfig;
	
	private RestTemplate rt;
	
	@PostConstruct
	private void init() {
		this.rt = new RestTemplate();
		this.componentDataFolder  = localDataFolder + "/" + COMPONENT_NAME;
		this.peersFolder = this.componentDataFolder + "/peer-certs";
		this.configFile = this.componentDataFolder + "/config.json";
		this.pemCer = this.componentDataFolder + "/cert.pem";
		this.pemKey = this.componentDataFolder + "/key.pem";
		new File( this.peersFolder ).mkdirs();
		logger.info("init " + this.componentDataFolder );
	}

	public boolean certAndKeysExists() {
		boolean result = ( new File( this.pemCer ).exists() && new File( this.pemKey ).exists() );
		return result;
	}
	
	// We have the CA created, Org name and Node name.
	// I think we can create the DataExchange key pair and sign the certificate with the CA.
	private void createCertificateAndKeys() {
		boolean stackIsLocked = localService.getMainConfig().getJSONObject("stackStatus").getBoolean("locked");
		String hostName = this.localService.getMainConfig().getString("hostName");
		String nodeName = this.localService.getMainConfig().getString("nodeName");
		boolean stackCertsWasCreated = this.localService.getPkiManager().caWasCreated();
		if( stackIsLocked && stackCertsWasCreated && !certAndKeysExists() ) {
		    String hostCn = "/CN="+hostName+"/O="+nodeName+"/OU=FireFly/OU=Multiparty Deployer Agent";
			this.localService.getPkiManager().createAndSignKeysAndCert( COMPONENT_NAME, hostCn , this.componentDataFolder );
		}
	}
	
	public String startContainer() {
		
		JSONObject container = getContainer();
		// If we have a container already, just start it and get out
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
		 
		// Well.. there is no container yet. Lets create and start one.
		JSONObject portBidings = new JSONObject();
		portBidings.put("10205", "3000/tcp");
		portBidings.put("10204", "3001/udp");
		
		JSONArray envs = new JSONArray();
		// Don't know what is the effect of this
		// envs.put("NODE_TLS_REJECT_UNAUTHORIZED='0'");
		
		// Change the log level this way makes the log to have no output.
		// Don't know why and will not care for now.
		// envs.put("LOG_LEVEL='DEBUG'");
		
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
	
	private JSONObject getContainer() {
		JSONObject container = containerManager.getContainer( COMPONENT_NAME ); 
		return container;
	}

	public String pullImage() {
		return imageManager.pullImage(COMPONENT_NAME, true );
	}
	
	public String getConfig( ) {
		JSONObject localAgentConfig = localService.getMainConfig();
		
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
		
		// Plus the local node config ( I need this server's IP and host )
		generalConfig.put("localAgentConfig", localAgentConfig );
		
		return generalConfig.toString(5);
	}

	public String addPeer( JSONObject peer ){
		this.loadConfig();
		this.componentConfig.getJSONArray("peers").put( peer );
		try {
			this.saveConfig();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		return this.componentConfig.toString();
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

	public String stopContainer() {
		return new JSONObject().put("result", this.containerManager.stopContainer(COMPONENT_NAME) ).toString();
	}

	public String restartContainer() {
		return new JSONObject().put("result", this.containerManager.reStartContainer( COMPONENT_NAME) ).toString();
	}

	public String getPeerId() {
		String peerId = "Wait...";
		try { peerId = this.requestData("http://dataexchange:3000/api/v1/id");	} catch ( Exception e ) {	}
		return peerId;
	}
	
	public String getStatus() {
		String status = "Wait...";
		try { status = this.requestData("http://dataexchange:3000/api/v1/status");	} catch ( Exception e ) {	}
		return status;
	}

	private String requestData( String endpoint ) {
		return rt.getForObject( endpoint, String.class);
	}

	public void connectClientToApi() throws Exception {
		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.add("Sec-WebSocket-Key", "SGVsbG8sIHdvcmxkIQ==");
		headers.add("Sec-WebSocket-Version", "13");
		headers.add("Sec-WebSocket-Extensions", "permessage-deflate; client_max_window_bits");
		URI uri = new URI("ws://"+COMPONENT_NAME+":3000");
		WebSocketClient client = new StandardWebSocketClient();
		CompletableFuture<WebSocketSession> future =  client.execute( new DXWebSocketHandler( this ), headers, uri);
		future.get();	
	}	

	public void processMessageFromDX( WebSocketSession session, JSONObject payload ) {
		System.out.println( "Received: " );
		System.out.println( payload.toString(5) );
		
		messagingTemplate.convertAndSend( "/data/dataexchange", new JSONObject(  ).put("payload", payload).toString() );
		
		
		JSONObject ack = new JSONObject();
		ack.put("action","ack").put("id", payload.getString("id") );

		System.out.println( "Responding: " );
		System.out.println( ack.toString(5) );
		try {
			session.sendMessage( new TextMessage( ack.toString() ) );
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
	public String sendMessage( String message ) throws Exception {
		JSONObject payload = new JSONObject();
		payload.put("message", message);
		payload.put("recipient", "FireFly");
		payload.put("requestId", UUID.randomUUID().toString() );

		System.out.println("Saindo " + payload.toString() );
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		RequestEntity<String> requestEntity = RequestEntity 
				.post( new URL("http://"+COMPONENT_NAME+":3000/api/v1/messages").toURI() ) 
				.contentType( MediaType.APPLICATION_JSON ) 
				.body( payload.toString() ); 
		
		return rt.exchange(requestEntity, String.class ).getBody();
	}
	
	public Resource getPeerCertificateFile() throws Exception {
	    Path path = Paths.get( this.pemCer );
	    ByteArrayResource resource = new ByteArrayResource( Files.readAllBytes( path ) );
	    return resource;	    
	}
	
}

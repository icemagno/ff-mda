package br.com.j1scorpii.ffmda.agent;

import java.lang.reflect.Type;
import java.net.URI;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import br.com.j1scorpii.ffmda.services.RemoteAgentService;
import br.com.j1scorpii.ffmda.util.RemoteAgentStatus;

public class RemoteAgent implements StompSessionHandler {
	private Logger logger = LoggerFactory.getLogger( RemoteAgent.class );
	private StompSession session = null;
	private WebSocketClient client;
	private URI uri;
	private WebSocketStompClient stompClient;
	private String address;
	private String uuid;
	private RemoteAgentStatus status = RemoteAgentStatus.NEW_ADDED;
	private String ipAddress;
	private String port;
	private String orgName;
	private String nodeName;
	private RemoteAgentService owner;
	
	// "ws://192.168.0.205:36780/ws"
	public RemoteAgent( String ipAddress, String port, String orgName, String nodeName, String uuid, RemoteAgentService owner ) throws Exception {
		this.uuid = uuid;
		this.owner = owner;
		this.orgName = orgName;
		this.nodeName = nodeName;
		this.address = "ws://" + ipAddress + ":" + port + "/ws";
		this.ipAddress = ipAddress;
		this.port = port;
		this.uri = new URI( address );
		this.client = new StandardWebSocketClient();
		this.stompClient = new WebSocketStompClient(client);
		this.stompClient.setMessageConverter( new StringMessageConverter() );
	}
	
	public String getOrgName() {
		return orgName;
	}
	
	public String getNodeName() {
		return nodeName;
	}
	
	public String getIpAddress() {
		return this.ipAddress;
	}
	
	public String getPort() {
		return port;
	}
	
	public String getId() {
		return this.uuid;
	}
	
	public String getStatus() {
		return status.toString();
	}
	
	public boolean isConnected() {
		return this.status == RemoteAgentStatus.CONNECTED;
	}
	
	public void connect() {
		if( this.status == RemoteAgentStatus.CONNECTING ) return;
		logger.debug("Trying to connect to " + this.address );
		this.status = RemoteAgentStatus.CONNECTING;
		stompClient.connectAsync( this.address, this , uri );
	}

	public void send( JSONObject payload ) {
		if( this.session != null ) {
			session.send("/master_agent", payload.toString() );
		}
	}
	
	@Override
	public Type getPayloadType(StompHeaders headers) {
		return String.class;
	}

	@Override
	public void handleFrame(StompHeaders headers, Object payload) {
		String msg = (String)payload;
		this.owner.receive( this.uuid, new JSONObject(msg), headers );
	}

	@Override
	public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
		logger.debug("Connected to " + this.address);
		status = RemoteAgentStatus.CONNECTED;
		this.session = session;
		send( new JSONObject().put("ping", "Connected!") );
		session.subscribe("/agent_master", this);
	}

	@Override
	public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
		exception.printStackTrace();
	}

	@Override
	public void handleTransportError(StompSession session, Throwable exception) {
		if (!session.isConnected() ) {
			logger.debug("Lost connection to " + this.address);
			status = RemoteAgentStatus.DISCONNECTED;
		}
	}
	
	
}

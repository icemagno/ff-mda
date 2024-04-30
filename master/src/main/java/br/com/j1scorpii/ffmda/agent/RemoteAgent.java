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
import br.com.j1scorpii.ffmda.util.FFMDAProtocol;
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
	private String hostName;
	private RemoteAgentService owner;
	
	public RemoteAgent( JSONObject agent, RemoteAgentService owner ) throws Exception {
		this.uuid = agent.getString("uuid");
		this.owner = owner;
		this.orgName = agent.getString("orgName");
		this.nodeName = agent.getString("nodeName");
		this.ipAddress = agent.getString("ipAddress");
		this.hostName = agent.getString("hostName");
		this.port = agent.getString("port");
		this.uri = new URI( address );
		this.client = new StandardWebSocketClient();
		this.stompClient = new WebSocketStompClient(client);
		this.stompClient.setMessageConverter( new StringMessageConverter() );
		this.address = "ws://" + ipAddress + ":" + port + "/ws";
	}
	
	public String getOrgName() {
		return orgName;
	}
	
	public String getHostName() {
		return hostName;
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
		logger.info("Trying to connect to " + this.address );
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
		logger.info("Connected to " + this.address);
		status = RemoteAgentStatus.CONNECTED;
		this.session = session;
		session.subscribe("/agent_master", this);
		send( new JSONObject().put("protocol", FFMDAProtocol.QUERY_DATA.toString() ) );
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
	
	// These may be confirmed / send by the node itself. We won't set it from Master
	// So they start as "Undefined" and filled after conect with the Remote Agent
	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}
	
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	
}

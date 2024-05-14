package br.com.j1scorpii.ffmda.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import br.com.j1scorpii.ffmda.agent.RemoteAgent;
import br.com.j1scorpii.ffmda.util.FFMDAProtocol;
import jakarta.annotation.PostConstruct;

@Service
@EnableScheduling
public class RemoteAgentService {
	private Logger logger = LoggerFactory.getLogger( RemoteAgentService.class );
	
	@Autowired private SimpMessagingTemplate messagingTemplate;
	@Autowired private BESUService besuService;
	@Autowired private LocalService localService;
	
	@Value("${ffmda.local.data.folder}")
	private String localDataFolder;	

	private String configFile;
	private List<RemoteAgent> agents;
	
	@PostConstruct
	private void init() {
		this.configFile	= localDataFolder + "/remote-agents.json";
		this.agents = new ArrayList<RemoteAgent>();
		this.loadConfig();
		logger.info("init " + agents.size() + " agents.");
	}
	
	private JSONObject getAgent( String ipAddress ) {
		for( RemoteAgent agent : this.agents ) {
			if( agent.getIpAddress().equals(ipAddress) ) return new JSONObject( agent );			
		}
		return new JSONObject();
	}
	
	public JSONObject addAgent( String data ) {
		JSONObject ag = new JSONObject( data ).getJSONObject("data");
		ag.put("orgName", "Undefined");
		ag.put("nodeName", "Undefined");
		ag.put("hostName", "Undefined");
		return addAgent( ag );
	}
	
	public JSONObject addAgent( JSONObject ag ) {
		// Check if it already exist ( the 'nodeName' attribute is present)
		JSONObject test = getAgent( ag.getString("ipAddress") ); 
		// If so then return it without do anything else.
		if( test.has("nodeName") ) return test;
		// We don't have this one. Register and wait for connect.
		// After connect the Remote Agent will send the rest of data that we started as 'Undefined'.
		try {
			String uuid = UUID.randomUUID().toString();
			ag.put("uuid", uuid);
			RemoteAgent ra = new RemoteAgent( ag, this );
			this.agents.add( ra );
			JSONObject newAgent = new JSONObject( ra );
			saveConfig();
			return newAgent;
		} catch ( Exception e ) {
			e.printStackTrace(); 
		}
		return new JSONObject();
	}
	
	private void saveConfig() {
		try {
			BufferedWriter writer = new BufferedWriter( new FileWriter( this.configFile ) );
			writer.write( this.getAgents().toString() );
			writer.close();
		} catch ( Exception e ) { 
			e.printStackTrace();
		}
	}

	private void loadConfig() {
		// Prevents FileNotFound
		if( !new File( this.configFile ).exists() ) return;
		// Try to load. We may not have any file if no agent was created yet.
		try {
			String content = readFile( configFile , StandardCharsets.UTF_8);
			JSONArray agentsConfig = new JSONArray(content);
			for( int x=0; x < agentsConfig.length(); x++ ) {
				JSONObject agent = agentsConfig.getJSONObject(x);
				// Shame! Shame! Shame!
				agent.put("uuid", agent.getString("id") );
				this.agents.add( new RemoteAgent( agent, this ) );
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
	private String readFile(String path, Charset encoding)  throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
	
	@Scheduled( fixedRate = 5000 )
	private void connectAgents() {
		
		// Will broadcast all agents to each other to close the net every 5 seconds
		// Also will try to connect to the registered agent if it is not yet connected
		for( RemoteAgent agent : this.agents ) {
			JSONObject agentJson = new JSONObject(agent);
			// Send the agent data to the front end
			messagingTemplate.convertAndSend( "/agent/status" , agentJson.toString() );
			// broadcast this agent data to all other agents
			broadcast( agentJson.put("protocol", FFMDAProtocol.AGENT_INFO.toString() ) );
			// try to connect to this agent
			try { if( !agent.isConnected() ) agent.connect(); } catch ( Exception e ) { }
			// if already connected then keep me up to date about its info
			if( agent.isConnected() ) agent.send( new JSONObject().put("protocol", FFMDAProtocol.QUERY_DATA.toString() ) );
		};
	}
	
	public JSONArray getAgents(){
		return new JSONArray( this.agents );
	}
	
	// Triggered when a message arrive from the Remote Agent
	// This is the entry point of the messages sent by agents
	public void receive( JSONObject payload, StompHeaders headers ) {
		// The Agent must obey the protocol otherwise the message will be discarded
		if( !payload.has("protocol") ) return;
		try {
			String protocolType = payload.getString("protocol");
			FFMDAProtocol protocol = FFMDAProtocol.valueOf(protocolType);
			switch (protocol) {
				case NODE_DATA: {
					// The agent response about itself. Here I'll receive the actual
					// host name, FF Node name and FF Org name. Let's update its internal registry
					assignNodeData( payload );
					break;
				}
				case COMMAND_ERROR: {
					processCommandError( payload );
				}
				default:
					break;
			}
		} catch ( Exception e ) {
			// Protocol error (unknown). 
			e.printStackTrace();
		}
	}
	
	private void processCommandError(JSONObject payload) {
		String uuid = payload.getString("uuid");
		logger.error( "ERROR: " + uuid + " " + payload.getJSONObject("command").getString("protocol") + " " + payload.getString("reason") );
	}

	// An agent sent his information data. I must take some actions
	private void assignNodeData(JSONObject payload) {
		String uuid = payload.getString("uuid");
		for( RemoteAgent agent : this.agents ) {
			if( agent.getId().equals(uuid) ) {
				agent.setOrgName( payload.getString("orgName") );
				agent.setNodeName( payload.getString("nodeName") );
				agent.setHostName( payload.getString("hostName") );
				agent.setBesuEnode( payload.getJSONObject("besuEnode") );
				
				System.out.println( payload.toString(5) );
				
				logger.info("DON'T FORGET TO RECEIVE THE BESU ENODE FROM AGENT ");
				// besuService.
				
				// Duh
				saveConfig();
			}
		}
	}
	
	// Command an agent to deploy a besu node there
	public String deployBesuNode() {
		JSONObject localAgentConfig = localService.getMainConfig();
		JSONObject besuData = localAgentConfig.getJSONObject("stackStatus").getJSONObject("besu");
		
		// ***************************************************
		// 		DON'T FORGET TO SEND THE BESU FILES FIRST !!!
		// ***************************************************
		
		// Send to all for instance ( I'm lazy to search for an UUID now )
		this.agents.forEach( ( agent ) -> {
			if( agent.isConnected() ) this.send( agent.getId(), new JSONObject()
				.put("protocol", FFMDAProtocol.DEPLOY_BESU.toString() )
				.put("imageName", besuData.getString("Image") )
			);
		});
		return localAgentConfig.toString(5);
		
	}

	// Send a message to an agent
	public void send( String uuid, JSONObject payload ) {
		this.agents.forEach( ( agent ) -> {
			if( agent.getId().equals(uuid) ) agent.send(payload);
		}); 
	}

	// Send agent data to all agents
	public void broadcast( JSONObject payload ) {
		this.agents.forEach( ( agent ) -> {
			// Do not send to myself
			if( !agent.getId().equals( payload.getString("id") ) ) agent.send(payload);
		}); 
	}

	// Triggered right after an agent is connected.
	// Let's ask it for its information ( containers and images info, host name, FF node name and FF org name ) 
	public void afterConnected(RemoteAgent remoteAgent, StompSession session, StompHeaders connectedHeaders) {
		remoteAgent.send( new JSONObject().put("protocol", FFMDAProtocol.QUERY_DATA.toString() ) );
	}

	
}

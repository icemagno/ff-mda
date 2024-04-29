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
				this.agents.add( new RemoteAgent( agent, this ) );
			}
		} catch ( Exception e ) {
			// I don't care ... just do nothing.
		}
	}
	
	private String readFile(String path, Charset encoding)  throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
	
	@Scheduled( fixedRate = 5000 )
	private void connectAgents() {
		this.agents.parallelStream().forEach( ( agent ) -> {
			messagingTemplate.convertAndSend( "/agent/status" , new JSONObject(agent).toString() );
			try { if( !agent.isConnected() ) agent.connect(); } catch ( Exception e ) { }
		}); 
	}
	
	public JSONArray getAgents(){
		return new JSONArray( this.agents );
	}

	
	// Triggered by the agent when message arrive
	public void receive( String uuid, JSONObject payload, StompHeaders headers ) {
		payload.put("uuid", uuid);
		if( payload.has("protocol") ) processProtocol( payload );
	}
	
	private void processProtocol( JSONObject payload ) {
		String protocolType = payload.getString("protocol");
		FFMDAProtocol protocol = FFMDAProtocol.valueOf(protocolType);
		
		switch (protocol) {
			case NODE_DATA: {
				assignNodeData( payload );
			}
			default:
				break;
		}
		
		// TEMP !!
		messagingTemplate.convertAndSend( "/agent/message" , payload.toString() );
	}	
	
	private void assignNodeData(JSONObject payload) {
		String uuid = payload.getString("uuid");
		this.agents.parallelStream().forEach( ( agent ) -> {
			if( agent.getId().equals(uuid) ) {
				agent.setOrgName( payload.getString("orgName") );
				agent.setNodeName( payload.getString("nodeName") );
				saveConfig();
			}
		});
	}

	// Call the agent to send a message
	public void send( String uuid, JSONObject payload ) {
		this.agents.forEach( ( agent ) -> {
			if( agent.getId().equals(uuid) ) agent.send(payload);
		}); 
	}

}

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
	private JSONArray agentsConfig = new JSONArray();
	
	@PostConstruct
	private void init() {
		this.configFile	= localDataFolder + "/remote-agents.json";
		this.agents = new ArrayList<RemoteAgent>();
		this.loadConfig();
		logger.info("init " + agents.size() + " agents.");
	}
	
	private JSONObject getAgent( String ipAddress ) {
		for( int x=0; x < this.agentsConfig.length(); x++ ) {
			JSONObject agent = this.agentsConfig.getJSONObject(x);
			if( agent.getString("ipAddress").equals(ipAddress) ) return agent;
		}
		return new JSONObject();
	}
	
	public JSONArray getAgentsConfig() {
		return agentsConfig;
	}
	
	public JSONObject addAgent(String data) {
		JSONObject ag = new JSONObject( data ).getJSONObject("data");
		return addAgent( 
			ag.getString("ipAddress"),
			ag.getString("port"), 
			ag.getString("orgName"),
			ag.getString("nodeName")
		);
	}
	
	public JSONObject addAgent( String ipAddress, String port, String orgName, String nodeName ) {
		JSONObject test = getAgent( ipAddress ); 
		if( test.has("nodeName") ) return test;
		try {
			String uuid = UUID.randomUUID().toString();
			JSONObject newAgent = new JSONObject()
					.put("uuid", uuid)
					.put("orgName", orgName)
					.put("nodeName", nodeName)
					.put("ipAddress", ipAddress)
					.put("port", port);
			this.agentsConfig.put( newAgent );
			saveConfig();
			RemoteAgent ra = new RemoteAgent( ipAddress, port, orgName, nodeName, uuid, this );
			this.agents.add( ra );
			return new JSONObject( ra );
		} catch ( Exception e ) {
			e.printStackTrace(); 
		}
		return new JSONObject();
	}
	
	private void saveConfig() throws Exception {
		BufferedWriter writer = new BufferedWriter( new FileWriter( this.configFile) );
		writer.write( this.agentsConfig.toString() );
		writer.close();			
	}

	private void loadConfig() {
		// Prevents FileNotFound
		if( !new File( this.configFile ).exists() ) return;
		// Try to load. We may not have any file if no agent was created yet.
		try {
			String content = readFile( configFile , StandardCharsets.UTF_8);
			this.agentsConfig = new JSONArray(content);
			for( int x=0; x < this.agentsConfig.length(); x++ ) {
				JSONObject agent = this.agentsConfig.getJSONObject(x);
				this.agents.add( new RemoteAgent( 
					agent.getString("ipAddress"), 
					agent.getString("port"), 
					agent.getString("orgName"), 
					agent.getString("nodeName"), 
					agent.getString("uuid"), 
					this ) 
				);
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
		messagingTemplate.convertAndSend( "/agent/message" , payload.toString() );
	}
	
	// Call the agent to send a message
	public void send( String uuid, JSONObject payload ) {
		this.agents.forEach( ( agent ) -> {
			if( agent.getId().equals(uuid) ) agent.send(payload);
		}); 
	}

}

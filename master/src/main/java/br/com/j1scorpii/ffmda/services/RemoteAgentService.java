package br.com.j1scorpii.ffmda.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
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
import br.com.j1scorpii.ffmda.enums.AgentKind;
import br.com.j1scorpii.ffmda.enums.ResultType;
import br.com.j1scorpii.ffmda.util.FFMDAProtocol;
import jakarta.annotation.PostConstruct;

@Service
@EnableScheduling
public class RemoteAgentService {
	private Logger logger = LoggerFactory.getLogger( RemoteAgentService.class );
	
	@Autowired private SimpMessagingTemplate messagingTemplate;
	@Autowired private BESUService besuService;
	@Autowired private LocalService localService;
	@Autowired private SecureChannelService secChannel;
	@Autowired private FileSenderService fileSender;
	
	@Value("${ffmda.local.data.folder}")
	private String localDataFolder;	
	
	private String agentFilesFolder;

	private String configFile;
	private List<RemoteAgent> agents;
	
	@PostConstruct
	private void init() {
		this.configFile	= localDataFolder + "/remote-agents.json";
		this.agentFilesFolder = localDataFolder + "/agents";
		this.agents = new ArrayList<RemoteAgent>();
		this.loadConfig();
		new File( this.agentFilesFolder ).mkdirs();
		logger.info("init " + agents.size() + " agents.");
	}
	
	private JSONObject getAgent( String ipAddress ) {
		for( RemoteAgent agent : this.agents ) {
			if( agent.getIpAddress().equals(ipAddress) ) return new JSONObject( agent );			
		}
		return new JSONObject();
	}
	public RemoteAgent getAgentById( String agentId ) {
		for( RemoteAgent agent : this.agents ) {
			if( agent.getId().equals(agentId) ) return agent ;			
		}
		return null;
	}

	// Called by the front end
	public JSONObject addAgent( String data ) {
		
		// Insert an empty object to be filled by the agent when connect  
		JSONObject ag = new JSONObject( data ).getJSONObject("data");
		ag.put("orgName", "Undefined");
		ag.put("nodeName", "Undefined");
		ag.put("hostName", "Undefined");
		
		// Check if it already exist ( the 'nodeName' attribute is present)
		// If so then return it without do anything else.
		JSONObject test = getAgent( ag.getString("ipAddress") ); 
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
			
			// Create a folder to store all this agent config files
			new File( this.agentFilesFolder + "/" + uuid ).mkdirs();
			
			// Create certificates and other files for this new agent
			configureFiles( ra );
			
			return newAgent;
		} catch ( Exception e ) {
			e.printStackTrace(); 
		}
		return new JSONObject();
	}
	
	// Create certificates and initial files for an agent
	private void configureFiles( RemoteAgent ag ) {
		try {
			String agentFolder = this.agentFilesFolder + "/" + ag.getId();
			String dxAgentFolder = agentFolder + "/dx";
			String besuAgentFolder = agentFolder + "/besu";
			
			File besuFolderF = new File( besuAgentFolder ); 
			besuFolderF.mkdirs();
			new File( dxAgentFolder ).mkdirs();
			
		    String hostCn = "/CN="+ag.getHostName()+"/O="+ag.getNodeName()+"/OU=FireFly/OU=Multiparty Deployer Agent";
			this.localService.getPkiManager().createAndSignKeysAndCert( ag.getId() + "/dataexchange", hostCn , dxAgentFolder );
			
			// Copy all local besu files to the agent's folder ( including local keys. It will be override below )
			File besuLocalDataFolder = new File( besuService.getDataFolder() );
			File[] listOfFiles = besuLocalDataFolder.listFiles();
			if( listOfFiles != null) {
				for (int i = 0; i < listOfFiles.length; i++) {
					if ( listOfFiles[i].isFile() ) {
						FileUtils.copyFileToDirectory( listOfFiles[i], besuFolderF );				  
					}
				}
			}
			
			// Override the keys 
			this.besuService.generateValidatorKeyPair( besuAgentFolder, ag.getNodeName() );
			
		} catch (Exception e) {
			e.printStackTrace();
		} 		
	}

	// Called by the front to recreate agent files...
	// We'll need to send all again to the agent's host
	public String recreateFiles( String what, String agentId ) {
		RemoteAgent ag = getAgentById(agentId);
		if( ag == null  ) return "NO_AGENT_FOUND";
		configureFiles( ag );
		sendFiles( what, agentId );
		return "OK";
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
			String content = readFile( configFile );
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
	
	private String readFile(String path)  throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, StandardCharsets.UTF_8 );
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
	// TODO: Decrypt message
	public void receive( JSONObject payloadEnc, StompHeaders headers ) {
		// Decrypt the message
		JSONObject payload = secChannel.decrypt( payloadEnc );
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
				case LOG: {
					processLog( payload );
					break;
				}
				case RESULT: {
					processCommandResult( payload );
					break;
				}
				case COMMAND_ERROR: {
					processCommandError( payload );
					break;
				}
				default:
					break;
			}
		} catch ( Exception e ) {
			// Protocol error (unknown). 
			e.printStackTrace();
		}
	}
	
	private void processLog(JSONObject payload) {
		String uuid = payload.getString("uuid");
		logger.info( "LOG: " + uuid );
		logger.info( payload.toString() );

		// Send this log to the Agent Control Panel page on Agent ID channel
		messagingTemplate.convertAndSend( "/agent/log/" + uuid , payload.toString() );
	}

	private void processCommandResult(JSONObject payload) {
		String uuid = payload.getString("uuid");
		logger.info( "RESULT: " + uuid + " " + payload.getString("command") );
		logger.info( payload.getJSONObject("result").toString() );
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
				agent.setBesuData( payload.getJSONObject("besu") );
				
				logger.info("DON'T FORGET TO RECEIVE THE BESU ENODE FROM AGENT ");
				// System.out.println( payload.toString(5) );
				
				// Duh
				saveConfig();

				// Send this data to the Agent Control Panel page on Agent ID channel
				messagingTemplate.convertAndSend( "/agent/data/" + agent.getId() , payload.toString() );

			}
		}
	}
	
	public String deployStart( String what, String agentId ) {
		AgentKind agentKind = AgentKind.valueOf(what);
		switch( agentKind ) {
		case besu: 
			return deployBesuNode( agentId );
		case core:
			return deployCoreNode( agentId );
		case dx:
			break;
		case evm:
			break;
		case ipfs:
			break;
		case psql:
			break;
		case sandbox:
			break;
		case signer:
			break;
		case tokens:
			break;
		}
		logger.error("Unknown agent " + what );
		return null;
	}
	
	private String deployCoreNode( String agentId ) {
		return "";
	}
	
	
	// Command an agent to deploy a besu node there
	private String deployBesuNode( String agentId ) {
		RemoteAgent ag = getAgentById(agentId);
		if( ag == null ) return "NO_AGENT_FOUND";
		
		JSONObject localAgentConfig = localService.getMainConfig();
		JSONObject besuData = localAgentConfig.getJSONObject("stackStatus").getJSONObject("besu");
		
		// Send all files to agent
		sendFiles( "besu", agentId );

		this.sendToAgent( ag.getId(), new JSONObject()
			.put("protocol", FFMDAProtocol.DEPLOY_BESU.toString() )
			.put("imageName", besuData.getString("Image") )
		);
		return localAgentConfig.toString(5);
		
	}

	// Send a message to an agent
	public void sendToAgent( String uuid, JSONObject payload ) {
		this.agents.forEach( ( agent ) -> {
			if( agent.getId().equals(uuid) ) agent.send( secChannel.encrypt( payload ) );
		}); 
	}

	// Send agent data to all other agents
	public void broadcast( JSONObject payload ) {
		this.agents.forEach( ( agent ) -> {
			// Do not send to myself
			if( !agent.getId().equals( payload.getString("id") ) ) agent.send( secChannel.encrypt( payload ) );
		}); 
	}

	// Triggered right after an agent is connected.
	// Let's ask it for its information ( containers and images info, host name, FF node name and FF org name ) 
	public void afterConnected(RemoteAgent remoteAgent, StompSession session, StompHeaders connectedHeaders) {
		sendToAgent( remoteAgent.getId(), new JSONObject().put("protocol", FFMDAProtocol.QUERY_DATA.toString() ) );
	}
	
	private String makeResult( String text, ResultType type ) {
		return new JSONObject()
				.put("result", text)
				.put("type", type.toString() )
				.toString();
	}

	// Send config files to an agent
	public String sendFiles( String what, String agentId ) {
		String agentFolder = this.agentFilesFolder + "/" + agentId;
		File dxAgentFolder = new File( agentFolder + "/dx" );
		File besuAgentFolder = new File( agentFolder + "/besu" );
		
		RemoteAgent ag = getAgentById(agentId);
		if( ag == null ) return makeResult("No Agent Connected", ResultType.ERROR );
		
		JSONObject thisNodeBlockChainData = new JSONObject( besuService.getBlockchainData() );

		if( ! thisNodeBlockChainData.has("enode")  ) {
			return makeResult("Can't connect to the local BESU node to take ENODE address. Is it running?", ResultType.ERROR );
		}
		
		System.out.println( thisNodeBlockChainData.getString("enode") );
		
	    // "enode://f6f0628abeced644e5549cc4fe8463202058271eef3b4ea0f4ddec898ea369744940eac0503e7f3f8f652919eef8dd5d94786370832c4ed295e21016a1f9f268@node-01:30303",

		
		System.out.println( "Besu Files: " );
		File[] besuFiles = besuAgentFolder.listFiles();
		if( besuFiles != null) {
			for (int i = 0; i < besuFiles.length; i++) {
				if ( besuFiles[i].isFile() ) {
					// fileSender.sendFile( "besu", ag, besuFiles[i].getAbsolutePath() );
					System.out.println( " > " + besuFiles[i].getAbsolutePath() );
					System.out.println( "   > " + besuFiles[i].getName() );
				}
			}
		}

		
		System.out.println( "DX Files: " );
		File[] dxFiles = dxAgentFolder.listFiles();
		if( dxFiles != null) {
			for (int i = 0; i < dxFiles.length; i++) {
				if ( dxFiles[i].isFile() ) {
					fileSender.sendFile( "dataexchange", ag, dxFiles[i].getAbsolutePath() );
				}
			}
		}
		
		
		return "ok";
	}

	
}

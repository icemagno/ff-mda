package br.com.j1scorpii.ffmda.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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
import br.com.j1scorpii.ffmda.enums.FFMDAProtocol;
import br.com.j1scorpii.ffmda.enums.ResultType;
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
			recreateBesuData( ra );
			recreateDxData( ra );
			
			return newAgent;
		} catch ( Exception e ) {
			e.printStackTrace(); 
		}
		return new JSONObject();
	}
	
	// Create certificates and initial files for an agent
	private void recreateBesuData( RemoteAgent ag ) {
		logger.info("will clone local BESU data to agent " + ag.getIpAddress() + " (" + ag.getId() + ")" );
		try {
			String agentFolder = this.agentFilesFolder + "/" + ag.getId();
			String besuAgentFolder = agentFolder + "/besu";
			File besuFolderF = new File( besuAgentFolder ); 
			besuFolderF.mkdirs();
			
			// Generate the key pair files 
			JSONObject vd = this.besuService.generateValidatorKeyPair( besuAgentFolder, ag.getNodeName() );
			String pubKey = vd.getString("pubKey");
			String ipAddress = ag.getIpAddress();
			
			String enode = besuService.makeEnodeAddress(ipAddress, pubKey);
			
			besuService.updatePermissionsFile(enode);
			besuService.updateStaticNode(enode, false);

			FileUtils.copyFileToDirectory( new File("permissions_config.toml") , besuFolderF );
			FileUtils.copyFileToDirectory( new File("config.toml") , besuFolderF );
			FileUtils.copyFileToDirectory( new File("static-nodes.json") , besuFolderF );
			FileUtils.copyFileToDirectory( new File("genesis.json") , besuFolderF );
			
			logger.info("Agent " + ag.getIpAddress() + " have ENODE address:");
			logger.info(enode);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void recreateDxData( RemoteAgent ag ) {
		logger.info("will create DataExchange data for agent " + ag.getIpAddress() + " (" + ag.getId() + ")" );
		try {
			String agentFolder = this.agentFilesFolder + "/" + ag.getId();
			String dxAgentFolder = agentFolder + "/dx";
			new File( dxAgentFolder ).mkdirs();
		    String hostCn = "/CN="+ag.getHostName()+"/O="+ag.getNodeName()+"/OU=FireFly/OU=Multiparty Deployer Agent";
			this.localService.getPkiManager().createAndSignKeysAndCert( ag.getId() + "/dataexchange", hostCn , dxAgentFolder );
		} catch (Exception e) {
			e.printStackTrace();
		} 		
	}

	// Called by the front to recreate agent files...
	// We'll need to send all again to the agent's host
	public String recreateFiles( String what, String agentId ) {
		RemoteAgent ag = getAgentById(agentId);
		if( ag == null  ) return "NO_AGENT_FOUND";
		recreateBesuData( ag );
		recreateDxData( ag );
		sendFilesToAgent( what, agentId );
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
		sendFilesToAgent( "besu", agentId );

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
	public void afterAgentIsConnected(RemoteAgent remoteAgent, StompSession session, StompHeaders connectedHeaders) {
		sendToAgent( remoteAgent.getId(), new JSONObject().put("protocol", FFMDAProtocol.QUERY_DATA.toString() ) );
	}
	
	// Format a JSON result to send to front end in response to a command
	private String makeResultToFront( String text, ResultType type ) {
		return new JSONObject()
				.put("result", text)
				.put("type", type.toString() )
				.toString();
	}

	// Send configuration files to an agent.
	// It will be a copy of my own files into the 'agentFilesFolder'
	// for each agent registered. I will modify and send these files
	// to configure the Agent's components (mostly BESU, IPFS and DX)
	public String sendFilesToAgent( String what, String agentId ) {
		try {
			String agentFolder = this.agentFilesFolder + "/" + agentId;
			File dxAgentFolder = new File( agentFolder + "/dx" );
			File besuAgentFolder = new File( agentFolder + "/besu" );
			File ipfsAgentFolder = new File( agentFolder + "/ipfs" );
			
			RemoteAgent ag = getAgentById(agentId);
			if( ag == null ) return makeResultToFront("No Agent Connected", ResultType.ERROR );
			
			JSONObject thisNodeBlockChainData = new JSONObject( besuService.getBlockchainData() );
	
			if( ! thisNodeBlockChainData.has("enode")  ) {
				return makeResultToFront("Can't connect to the local BESU node to take ENODE address. Is it running?", ResultType.ERROR );
			}

			// Clone BESU configuration again. The user may have changed something here.
			recreateBesuData(ag);
			
			// Prepare the Bootnodes option to append to the remote agent BESU config.toml file
			String localEnode = thisNodeBlockChainData.getString("enode");
			String bootNodeOption = "bootnodes=[\"" + localEnode + "\"]";
			updateBootNodesBeforeSend( agentFolder + "/besu/config.toml", bootNodeOption );
			
			logger.info("sending files from " + agentFolder );
			
			// Send BESU configuration
			logger.info(" > sending BESU configuration");
			sendAgentFilesFromFolder( besuAgentFolder.listFiles(), "besu", ag );

			// Send DX configuration
			logger.info(" > sending DataExchange configuration");
			sendAgentFilesFromFolder( dxAgentFolder.listFiles(), "dataexchange", ag );
			
			// Send IPFS configuration
			logger.info(" > sending IPFS configuration");
			sendAgentFilesFromFolder( ipfsAgentFolder.listFiles(), "ipfs", ag );
			
		} catch (Exception e) {
			return makeResultToFront( e.getMessage(), ResultType.ERROR );
		}	
		return makeResultToFront( "All configuration flies sent to remote agent.", ResultType.SUCCESS );
	}

	// Actually send the files from local component's folder
	private void sendAgentFilesFromFolder( File[] files, String componentName, RemoteAgent ag ) {
		if( files != null) {
			for (int i = 0; i < files.length; i++) {
				String fileName = files[i].getName();
				String absolutePath = files[i].getAbsolutePath();
				if ( files[i].isFile() ) {
					fileSender.sendFile( componentName, ag, fileName, absolutePath );
					logger.info("   > " + fileName );
				}
			}
		}		
	}
	
	// Update the config.toml file ( the BESU startup options file ) before send it to
	// the remote node. We need to put this local BESU node as the Boot node for all BESU network nodes.
	// This method will append the "bootnodes" option to the startup 'config.toml' file with this local BESU ENODE
	// address and send it to the remote Agent. This will make all BESU nodes use this local BESU node as the chain boot node.
	private void updateBootNodesBeforeSend( String absolutePath, String bootNodeOption ) throws Exception {
		// Check if we already done this. The user may want to send these files at any time.
		Scanner s = new Scanner(new File( absolutePath ));
		while (s.hasNext()){
		    String line = s.next();
		    if( line.contains("bootnodes") ) {
		    	s.close();
		    	return;
		    }
		}
		s.close();		
		// Append this local BESU enode to the remote BESU startup config
		// as the Bootnode
	    Files.write( Paths.get( absolutePath ), bootNodeOption.getBytes(), StandardOpenOption.APPEND );
	}

	
}

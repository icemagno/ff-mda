package br.com.j1scorpii.ffmda.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import br.com.j1scorpii.ffmda.enums.FFMDAProtocol;
import jakarta.annotation.PostConstruct;

@Service
@Order(value = 0)
@EnableScheduling
public class CommService {
	private Logger logger = LoggerFactory.getLogger( CommService.class );
	
	@Autowired private BESUService besuService;
	@Autowired private IPFSService ipfsService;
	@Autowired private DataExchangeService dataExchangeService;
	@Autowired private SimpMessagingTemplate messagingTemplate;
	@Autowired private SecureChannelService secChannel;
	
	@Value("${ffmda.data.folder}")
	private String localDataFolder;

	@Value("${ffmda.node.name}")
	private String nodeName;

	@Value("${ffmda.org.name}")
	private String orgName;
	
	@Value("${ffmda.host.name}")
	private String hostName;
	
	private String commChannel = "/agent_master";
	
	@PostConstruct
	private void init() {
		logger.info("init " + localDataFolder);
		new File( localDataFolder ).mkdirs();
	}
	
	// This is the entry point of all messages sent by the Master to me.
	// The Master must obey the protocol otherwise the message will be discarded
	// This method is called by CommController which register the WS channel listener
	public void receive(String message, MessageHeaders messageHeaders) {
		JSONObject payload = new JSONObject( secChannel.decrypt( message ) );
		if( !payload.has("protocol") ) return;
		try {
			String protocolType = payload.getString("protocol");
			FFMDAProtocol protocol = FFMDAProtocol.valueOf(protocolType);
			switch (protocol) {
				case FILE: {
					receiveFileFromMaster( payload );
					break;
				}
				case QUERY_DATA: {
					respondQueryData( );
					break;
				}
				case AGENT_INFO: {
					processAgentInfo( payload );
					break;
				}
				case DEPLOY_BESU: {
					deployBesu( payload );
					break;
				}
				default:
					break;
			}		
		} catch ( Exception e ) {
			// Wrong protocol
			e.printStackTrace();
		}
	}
	
	// The Master sent a file. Save it.
	private void receiveFileFromMaster(JSONObject payload) {
		JSONObject filePayload = secChannel.decrypt(payload);
		if( filePayload.has("fileContent") ) {
			try {
				String fileContent = filePayload.getString("fileContent").replaceAll("\\\\n", "").replaceAll("\\\\\"", "\"");
				String fileName = filePayload.getString("fileName");
				String componentName = filePayload.getString("component");
				String targetFolder = null;
				
				if( componentName.equals("besu") ) targetFolder = besuService.getComponentDataFolder();
				if( componentName.equals("dataexchange") ) targetFolder = dataExchangeService.getComponentDataFolder();
				if( componentName.equals("ipfs") ) targetFolder = ipfsService.getComponentDataFolder();
				
				if( targetFolder == null ) {
					commandError( payload, "Unknown component name: '" + componentName + "'" );
					return;
				}				
				
				logger.info("received file " + fileName + " for component '" + componentName + "'");
				logger.info("  > " + targetFolder + "/" + fileName );
				
				new File( targetFolder ).mkdirs();
				BufferedWriter writer = new BufferedWriter( new FileWriter( targetFolder + "/" + fileName ) );
				writer.write( fileContent );
				writer.close();
				
			} catch ( Exception e ) { 
				e.printStackTrace();
			}			
		}
	}

	// The Master is commanding me to start a BESU node.
	// Let's deploy one based on the Master instructions
	private void deployBesu( JSONObject payload ) {
		// check if master is telling me to deploy a besu node but give 
		// no information about it
		if( !payload.has("imageName") ) {
			commandError(payload, "No BESU image name was given");
			return;
		}
		JSONObject result = besuService.deploy( payload.getString("imageName"), commChannel );
		sendResult( payload, result );
	}

	
	private void sendResult(JSONObject payload, JSONObject result) {
		this.sendToMaster( new JSONObject(  )
			.put("protocol", FFMDAProtocol.RESULT.toString() )
			.put("command", payload.getString("protocol") )
			.put("result", result )
		);
	}

	// I can't understand or execute the command from master.
	// Throw it back to inform the error
	private void commandError( JSONObject command, String reason ) {
		this.sendToMaster( new JSONObject(  )
			.put("protocol", FFMDAProtocol.COMMAND_ERROR.toString() )
			.put("command", command )
			.put("reason", reason)
		);
	}
	
	// Send messages to Master thru main channel
	public void sendToMaster( JSONObject payload ) {
		messagingTemplate.convertAndSend( commChannel, secChannel.encrypt( payload.toString() ) );
	}
	
	// The Master is telling me about a brother Agent found on network. 
	// Let's know how glorious is my brother!
	private void processAgentInfo(JSONObject payload) {
		System.out.println("A brother Agent...");
		System.out.println("--- Don't forget to register it's BESU enode as a static peer");
		System.out.println( payload.toString(5) );
	}

	// The Master is asking me about who am I.
	// Don't let it waiting! Tell him how glorious I am and let him to 
	// spread this to all my brothers on network.
	private void respondQueryData() {
		this.sendToMaster( new JSONObject(  )
			.put("protocol", FFMDAProtocol.NODE_DATA.toString() )
			.put("nodeName", this.nodeName )
			.put("hostName", this.hostName )
			.put("orgName", this.orgName )
			.put("besu", besuService.getConfig() )
		);
	}
	
}

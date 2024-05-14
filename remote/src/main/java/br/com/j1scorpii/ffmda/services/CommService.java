package br.com.j1scorpii.ffmda.services;

import java.io.File;

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

import br.com.j1scorpii.ffmda.util.FFMDAProtocol;
import jakarta.annotation.PostConstruct;

@Service
@Order(value = 0)
@EnableScheduling
public class CommService {
	private Logger logger = LoggerFactory.getLogger( CommService.class );
	
	@Autowired private BESUService besuService;
	@Autowired private SimpMessagingTemplate messagingTemplate;
	
	@Value("${ffmda.data.folder}")
	private String localDataFolder;

	@Value("${ffmda.node.name}")
	private String nodeName;

	@Value("${ffmda.org.name}")
	private String orgName;
	
	@Value("${ffmda.host.name}")
	private String hostName;
	
	@PostConstruct
	private void init() {
		logger.info("init " + localDataFolder);
		new File( localDataFolder ).mkdirs();
	}
	
	// This is the entry point of all messages sent by the Master to me.
	// The Master must obey the protocol otherwise the message will be discarded
	public void receive(String message, MessageHeaders messageHeaders) {
		JSONObject payload = new JSONObject( message );
		if( !payload.has("protocol") ) return;
		try {
			String protocolType = payload.getString("protocol");
			FFMDAProtocol protocol = FFMDAProtocol.valueOf(protocolType);
			switch (protocol) {
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
	
	// The Master is commanding me to start a BESU node.
	// Let's deploy one based on the Master instructions
	private void deployBesu( JSONObject payload ) {
		// check if master is telling me to deploy a besu node but give 
		// no information about it
		if( !payload.has("imageName") ) {
			commandError(payload, "No BESU image name was given");
			return;
		}
		
		// Check if we already have all files needed by the BESU
		// to start...
		// if( no files here) commandError() ... 
		
		System.out.println("Must deploy a BESU node....");
		System.out.println( payload.toString(5) );
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
	private void sendToMaster( JSONObject payload ) {
		System.out.println("SEND TO MASTER");
		System.out.println( payload.toString(5) );
		messagingTemplate.convertAndSend( "/agent_master", payload.toString() );
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
			.put("besuEnode", besuService.getNodeID() )
		);
	}
	
}

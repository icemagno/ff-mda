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

import br.com.j1scorpii.ffmda.util.EtcHosts;
import br.com.j1scorpii.ffmda.util.FFMDAProtocol;
import jakarta.annotation.PostConstruct;

@Service
@Order(value = 0)
@EnableScheduling
public class CommService {
	private Logger logger = LoggerFactory.getLogger( CommService.class );
	
	@Autowired private SimpMessagingTemplate messagingTemplate;
	
	@Value("${ffmda.data.folder}")
	private String localDataFolder;

	@Value("${ffmda.node.name}")
	private String nodeName;

	@Value("${ffmda.org.name}")
	private String orgName;
	
	@Value("${ffmda.host.name}")
	private String hostName;
	
	@Value("${ffmda.host.address}")
	private String hostAddress;
	
	private EtcHosts hosts;
	
	@PostConstruct
	private void init() {
		logger.info("init " + localDataFolder);
		new File( localDataFolder ).mkdirs();

		hosts = new EtcHosts( localDataFolder );
		hosts.print();

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
				default:
					break;
			}		
		} catch ( Exception e ) {
			// Wrong protocol
			e.printStackTrace();
		}
	}

	
	// The Master is telling me about a brother Agent found on network. 
	// Let's know my brother!
	// I must register its host name on my /etc/hosts ....
	private void processAgentInfo(JSONObject payload) {
		System.out.println("A brother Agent...");
		System.out.println( payload.toString(5) );

		String ipAddress = payload.getString("ipAddress");
		if( !hosts.getHosts().containsKey( ipAddress ) ) {
			hosts.getHosts().put( ipAddress, payload.getString("hostName") );
			hosts.save();
		}
		
		
	}

	// The Master is asking me about who am I.
	// Don't let it waiting! Tell him how glorious I am and let him to 
	// spread this to all my brothers on network.
	private void respondQueryData() {
		messagingTemplate.convertAndSend( "/agent_master", new JSONObject(  )
			.put("protocol", FFMDAProtocol.NODE_DATA.toString() )
			.put("nodeName", this.nodeName )
			.put("hostName", this.hostName )
			.put("orgName", this.orgName )
			.put("hostAddress", this.hostAddress )
			.toString() 
		);
	}
	
}

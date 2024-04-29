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
	
	@PostConstruct
	private void init() {
		logger.info("init " + localDataFolder);
		new File( localDataFolder ).mkdirs();
	}
	
	public void receive(String message, MessageHeaders messageHeaders) {
		JSONObject payload = new JSONObject( message );
		if( payload.has("protocol") ) processProtocol( payload );
	}

	private void processProtocol( JSONObject payload ) {
		String protocolType = payload.getString("protocol");
		FFMDAProtocol protocol = FFMDAProtocol.valueOf(protocolType);
		switch (protocol) {
			case QUERY_DATA: {
				respondQueryData( );
			}
			default:
				break;
		}		
	}
	
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

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
	
	@Scheduled(fixedDelay = 4000 )
	private void ping() {
		messagingTemplate.convertAndSend( "/ping", new JSONObject(  )
			.put("nodeName", this.nodeName )
			.put("hostName", this.hostName )
			.put("orgName", this.orgName )
			.put("hostAddress", this.hostAddress )
			.toString() 
		);
	}

	public void receive(String message, MessageHeaders messageHeaders) {
		String sessionId = new JSONObject( messageHeaders ).getString("simpSessionId");
		System.out.println( "[" + sessionId +  "]  Recebido pelo CommController (WebSocket) " );
		System.out.println( message );		
	}
	
}

package br.com.j1scorpii.ffmda.services;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import br.com.j1scorpii.ffmda.util.AgentWebSocketHandler;
import jakarta.annotation.PostConstruct;

@Service
public class RemoteCommService {

	private StompSession session = null;
	
	@PostConstruct
	private void init() {
		
		try {
			URI uri = new URI("ws://192.168.0.205:36780/ws");
			WebSocketClient client = new StandardWebSocketClient();
			WebSocketStompClient stompClient = new WebSocketStompClient(client);
			stompClient.setMessageConverter( new StringMessageConverter() );
			CompletableFuture<StompSession> future = stompClient.connectAsync("ws://192.168.0.205:36780/ws", new AgentWebSocketHandler(this) , uri);
			future.get();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
	}
	public void setSession(StompSession session) {
		this.session = session;
		session.send("/main_channel", new JSONObject().put("ping", "Connected!").toString() );
	}
	
	@Scheduled( fixedRate = 4000 )
	private void ping() {
		if( this.session != null ) {
			session.send("/main_channel", new JSONObject().put("ping", "I am Master").toString() );
		}
	}

	public void processMessageFromAgent( JSONObject jsonObject ) {
		System.out.println( jsonObject.toString(5) );
	}
	
}

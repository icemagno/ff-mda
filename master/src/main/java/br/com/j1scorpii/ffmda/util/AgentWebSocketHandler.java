package br.com.j1scorpii.ffmda.util;

import java.lang.reflect.Type;

import org.json.JSONObject;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;

import br.com.j1scorpii.ffmda.services.RemoteCommService;

public class AgentWebSocketHandler implements StompSessionHandler {
	private RemoteCommService owner;
	
	public AgentWebSocketHandler( RemoteCommService owner ) {
		this.owner = owner;
	}

	@Override
	public Type getPayloadType(StompHeaders headers) {
		return String.class;
	}

	@Override
	public void handleFrame(StompHeaders headers, Object payload) {
		String msg = (String)payload;
		this.owner.processMessageFromAgent( new JSONObject(msg) );
	}

	@Override
	public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
		this.owner.setSession(session);
	    session.subscribe("/ping", this);
	}

	@Override
	public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
		exception.printStackTrace();
	}

	@Override
	public void handleTransportError(StompSession session, Throwable exception) {
		exception.printStackTrace();
	}
	
}

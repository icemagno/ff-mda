package br.com.j1scorpii.ffmda.util;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import br.com.j1scorpii.ffmda.services.DataExchangeService;

public class DXWebSocketHandler extends AbstractWebSocketHandler {
	private DataExchangeService owner;
	private Logger logger = LoggerFactory.getLogger( DXWebSocketHandler.class );
	
	public DXWebSocketHandler( DataExchangeService owner ) {
		this.owner = owner;
	}
	
	@Override
	public boolean supportsPartialMessages() {
		return super.supportsPartialMessages();
	}
	
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		System.out.println( message.getPayload() );
		owner.processMessageFromDX( new JSONObject( message.getPayload() ) );
	}
	
	@Override
	protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
		logger.info("pong message");
	}
	
	@Override
	protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
		//
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		logger.error( exception.getMessage() );
	}
	
	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		super.handleMessage(session, message);
	}
	
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		logger.info("connected " + session.getId() );
	}
	
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		logger.info("disconnected");
	}
	
}

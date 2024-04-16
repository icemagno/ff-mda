package br.com.j1scorpii.ffmda.util;

import java.lang.reflect.Type;

import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

public class MyStompSessionHandler extends StompSessionHandlerAdapter {

	@Override
	public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
		System.out.println("connected");
		
	}	
	
	@Override
	public Type getPayloadType(StompHeaders headers) {
		return String.class;
	}
	
	@Override
	public void handleFrame(StompHeaders headers, Object payload) {
	    String msg = (String) payload;
	    System.out.println( msg );
	}	
	
}

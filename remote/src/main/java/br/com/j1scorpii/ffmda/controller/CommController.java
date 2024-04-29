package br.com.j1scorpii.ffmda.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import br.com.j1scorpii.ffmda.services.CommService;

@Controller
public class CommController {

	@Autowired private CommService localService;
	
	@MessageMapping("/master_agent")
	public void receiveData(@Payload String message, MessageHeaders messageHeaders) {
		localService.receive( message, messageHeaders );
	}	

	
}

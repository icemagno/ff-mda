package br.com.j1scorpii.ffmda.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.j1scorpii.ffmda.services.RemoteAgentService;

@RestController
@RequestMapping(value="/v1/agent")
public class RemoteAgentController {
	
	@Autowired private RemoteAgentService remoteAgentService;
	
    @GetMapping( value="/config", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> getConfig( ) throws Exception {
    	return new ResponseEntity<String>( this.remoteAgentService.getAgentsConfig().toString() , HttpStatus.OK);
    }  	

    @GetMapping( value="/list", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> getAgents( ) throws Exception {
    	return new ResponseEntity<String>( this.remoteAgentService.getAgents().toString() , HttpStatus.OK);
    }  	
    
    @PostMapping( value="/add", consumes= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> addAgent( @RequestBody String data ) throws Exception {
    	String result = this.remoteAgentService.addAgent(data).toString();
    	return new ResponseEntity<String>( result , HttpStatus.OK);
    }  	
    
}

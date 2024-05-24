package br.com.j1scorpii.ffmda.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.j1scorpii.ffmda.services.RemoteAgentService;

@RestController
@RequestMapping(value="/v1/agent")
public class RemoteAgentController {
	
	@Autowired private RemoteAgentService remoteAgentService;
	
    @GetMapping( value="/list", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> getAgents( ) throws Exception {
    	return new ResponseEntity<String>( this.remoteAgentService.getAgents().toString() , HttpStatus.OK);
    }  	

    @GetMapping( value="/deploy/{agentId}", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> deploy( @PathVariable String agentId ) throws Exception {
    	return new ResponseEntity<String>( this.remoteAgentService.deployBesuNode( agentId ) , HttpStatus.OK);
    }  	
    
    @GetMapping( value="/delete/{agentId}", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> delete(  @PathVariable String agentId ) throws Exception {
    	return new ResponseEntity<String>( agentId , HttpStatus.OK);
    }  	
    
    @PostMapping( value="/add", consumes= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> addAgent( @RequestBody String data ) throws Exception {
    	String result = this.remoteAgentService.addAgent(data).toString();
    	return new ResponseEntity<String>( result , HttpStatus.OK);
    }  	

    @GetMapping( value="/files/send/{agentId}", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> sendFiles( @PathVariable String agentId ) throws Exception {
    	return new ResponseEntity<String>( this.remoteAgentService.sendFiles( agentId ) , HttpStatus.OK);
    }  	
    
    @GetMapping( value="/files/recreate/{agentId}", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> recreateFiles( @PathVariable String agentId ) throws Exception {
    	return new ResponseEntity<String>( this.remoteAgentService.recreateFiles( agentId ) , HttpStatus.OK);
    }  	
    
}

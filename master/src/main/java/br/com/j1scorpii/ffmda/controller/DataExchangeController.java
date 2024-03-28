package br.com.j1scorpii.ffmda.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.j1scorpii.ffmda.services.DataExchangeService;

@RestController
@RequestMapping(value="/v1/dataexchange")
public class DataExchangeController {
	
	@Autowired private DataExchangeService dataExchangeService;

	// Check if we have the image already
    @GetMapping( value="/image/pulled", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> imagePulled( ) {
    	return new ResponseEntity<String>( this.dataExchangeService.imagePulled().toString() , HttpStatus.OK);
    }	

    @GetMapping( value="/image/pull", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> pullImage( ) {
    	return new ResponseEntity<String>( this.dataExchangeService.pullImage() , HttpStatus.OK);
    }    
    
    @GetMapping( value="/config/get", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> getConfig( ) {
    	return new ResponseEntity<String>( this.dataExchangeService.getConfig() , HttpStatus.OK);
    }    
    
    // Get the container information if we have any. Empty JSON object if not.
    @GetMapping( value="/container/get", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> getContainer( ) {
    	return new ResponseEntity<String>( this.dataExchangeService.getContainer().toString() , HttpStatus.OK);
    }	

    @GetMapping( value="/container/log", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> getContainerLog( ) {
    	return new ResponseEntity<String>( this.dataExchangeService.getContainerLog() , HttpStatus.OK);
    }	
    
    @GetMapping( value="/container/start", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> startContainer( ) {
    	return new ResponseEntity<String>( this.dataExchangeService.startContainer(), HttpStatus.OK);
    }    

    @GetMapping( value="/container/stop", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> stopContainer( ) {
    	return new ResponseEntity<String>( this.dataExchangeService.stopContainer(), HttpStatus.OK);
    }        
    
    @GetMapping( value="/container/restart", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> restartContainer( ) {
    	return new ResponseEntity<String>( this.dataExchangeService.restartContainer(), HttpStatus.OK);
    }        
      
}

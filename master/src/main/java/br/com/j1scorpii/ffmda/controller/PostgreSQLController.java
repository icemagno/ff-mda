package br.com.j1scorpii.ffmda.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.j1scorpii.ffmda.services.PostgreSQLService;

@RestController
@RequestMapping(value="/v1/postgresql")
public class PostgreSQLController {
	
	@Autowired private PostgreSQLService postgreSQLService;

	// Check if we have the image already
    @GetMapping( value="/image/pulled", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> imagePulled( ) {
    	return new ResponseEntity<String>( this.postgreSQLService.imagePulled().toString() , HttpStatus.OK);
    }	

    @GetMapping( value="/image/pull", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> pullImage( ) {
    	return new ResponseEntity<String>( this.postgreSQLService.pullImage() , HttpStatus.OK);
    }    
    
    @GetMapping( value="/config/get", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> getConfig( ) {
    	return new ResponseEntity<String>( this.postgreSQLService.getConfig() , HttpStatus.OK);
    }    
    
    @GetMapping( value="/container/start", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> startContainer( ) {
    	return new ResponseEntity<String>( this.postgreSQLService.startContainer(), HttpStatus.OK);
    }    

    
}

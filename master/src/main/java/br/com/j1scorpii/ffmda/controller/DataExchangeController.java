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
    	return new ResponseEntity<>( this.dataExchangeService.imagePulled() , HttpStatus.OK);
    }	

    // Get the container information if we have any. Empty JSON object if not.
    @GetMapping( value="/container/get", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> getContainer( ) {
    	return new ResponseEntity<>( this.dataExchangeService.getContainer() , HttpStatus.OK);
    }	
    
    
}

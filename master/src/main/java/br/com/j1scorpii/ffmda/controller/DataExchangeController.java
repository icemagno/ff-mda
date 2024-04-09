package br.com.j1scorpii.ffmda.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.j1scorpii.ffmda.services.DataExchangeService;
import jakarta.servlet.http.HttpServletResponse;

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
    
    // Get Data Exchange Peer ID
    @GetMapping( value="/peer/id", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> getPeerId( ) {
    	return new ResponseEntity<String>( this.dataExchangeService.getPeerId() , HttpStatus.OK);
    }    
    
    // Download peer certificate file (public key)
    @GetMapping( value="/peer/certificate", produces= MediaType.APPLICATION_OCTET_STREAM_VALUE )
    public @ResponseBody Resource getPeerCertificateFile( HttpServletResponse response ) throws Exception {
    	response.setHeader("Content-Disposition", "attachment; filename=peer.cer");
    	return this.dataExchangeService.getPeerCertificateFile();
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

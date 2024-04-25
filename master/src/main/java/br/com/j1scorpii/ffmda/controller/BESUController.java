package br.com.j1scorpii.ffmda.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.j1scorpii.ffmda.services.BESUService;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping(value="/v1/besu")
public class BESUController {
	
	@Autowired private BESUService besuService;
	
    // Download peer certificate file (public key)
    @GetMapping( value="/config/file", produces= MediaType.APPLICATION_OCTET_STREAM_VALUE )
    public @ResponseBody Resource downloadFile( @RequestParam (value="name",required=true) String fileName, HttpServletResponse response ) throws Exception {
    	return this.besuService.downloadFile( fileName, response );
    }  	
	
	// Check if we have the image already
    @GetMapping( value="/image/pulled", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> imagePulled( ) {
    	return new ResponseEntity<String>( this.besuService.imagePulled().toString() , HttpStatus.OK);
    }

    @GetMapping( value="/image/pull", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> pullImage( ) {
    	return new ResponseEntity<String>( this.besuService.pullImage() , HttpStatus.OK);
    }
    
    @GetMapping( value="/config/get", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> getConfig( ) {
    	return new ResponseEntity<String>( this.besuService.getConfig() , HttpStatus.OK);
    }    

    @GetMapping( value="/container/start", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> startContainer( ) {
    	return new ResponseEntity<String>( this.besuService.startContainer(), HttpStatus.OK);
    }    

      
}

package br.com.j1scorpii.ffmda.controller;


import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.j1scorpii.ffmda.services.LocalService;

@RestController
@RequestMapping(value="/v1")
public class LocalAgentController {

	@Autowired private LocalService localService;
	
	// Return the config data
    @GetMapping( value="/config/get", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> config( ) {
    	return new ResponseEntity<>( this.localService.getAgentConfig().toString() , HttpStatus.OK);
    }
    

    // Reload config data from disk
    @GetMapping( value="/config/reload", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> reloadConfig( ) {
    	this.localService.reloadConfig();
    	return new ResponseEntity<>( this.localService.getAgentConfig().toString() , HttpStatus.OK);
    }

    // Save Organization name and Node name into config file
    @PostMapping( value="/org/save", consumes= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> saveOrgData( @RequestBody String data  ) {
    	try {
    		JSONObject result = this.localService.saveOrgData( data );
    		return new ResponseEntity<>( result.toString(), HttpStatus.OK );
    	} catch (Exception e) {
    		e.printStackTrace();
    		return new ResponseEntity<>( e.getMessage() , HttpStatus.INTERNAL_SERVER_ERROR );
		}
    }    
    
}

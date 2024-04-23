package br.com.j1scorpii.ffmda.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.j1scorpii.ffmda.services.IPFSService;

@RestController
@RequestMapping(value="/v1/ipfs")
public class IPFSController {
	
	@Autowired private IPFSService ipfsService;
	
	// Check if we have the image already
    @GetMapping( value="/image/pulled", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> imagePulled( ) {
    	return new ResponseEntity<String>( this.ipfsService.imagePulled().toString() , HttpStatus.OK);
    }

	@GetMapping(value = "/peers/add", produces=MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> containerExec( 
    		@RequestParam (value="peerid",required=true) String peerId,
    		@RequestParam (value="peerip",required=true) String peerIp,
    		@RequestParam (value="peerport",required=true) String peerPort) {
		return new ResponseEntity<String>( this.ipfsService.addPeer(peerIp, peerPort, peerId) , HttpStatus.OK);
    }	    

    @GetMapping( value="/image/pull", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> pullImage( ) {
    	return new ResponseEntity<String>( this.ipfsService.pullImage() , HttpStatus.OK);
    }
    
    @GetMapping( value="/config/get", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> getConfig( ) {
    	return new ResponseEntity<String>( this.ipfsService.getConfig() , HttpStatus.OK);
    }    

    @GetMapping( value="/container/start", produces= MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> startContainer( ) {
    	return new ResponseEntity<String>( this.ipfsService.startContainer(), HttpStatus.OK);
    }    

      
}

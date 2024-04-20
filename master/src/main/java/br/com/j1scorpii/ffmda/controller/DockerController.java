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

import br.com.j1scorpii.ffmda.services.ContainerManager;
import br.com.j1scorpii.ffmda.services.ImageManager;

@RestController
@RequestMapping(value="/v1")
public class DockerController {
	
	@Autowired private ContainerManager containerManager; 
	@Autowired private ImageManager imageManager;	

	// ****************************  CONTEINERES  ***************************************
	
	@GetMapping(value = "/container/stats", produces=MediaType.APPLICATION_JSON_VALUE )
    public @ResponseBody String containerStats( @RequestParam (value="container",required=true) String containerName ) {
		return containerManager.getContainerStats(containerName);
    }	

	@GetMapping(value = "/container/list", produces=MediaType.APPLICATION_JSON_VALUE )
    public @ResponseBody String containerList( ) {
		return containerManager.getContainers();
    }	

	@GetMapping(value = "/container/log", produces=MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<String> containerLogs( @RequestParam (value="container",required=true) String containerName ) {
		return new ResponseEntity<String>( containerManager.getLog(containerName ), HttpStatus.OK );
    }	

	
	// ****************************  IMAGENS ***************************************
	
	@GetMapping(value = "/image/list", produces=MediaType.APPLICATION_JSON_VALUE )
    public @ResponseBody String imageList( ) {
		return imageManager.getImages();
    }	
	
	
}

package br.com.j1scorpii.ffmda.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.j1scorpii.ffmda.services.LocalService;

@RestController
@RequestMapping(value="/v1")
public class LocalAgentController {

	@Autowired private LocalService localService;
	
    @GetMapping( value="/config", produces= MediaType.APPLICATION_JSON_VALUE )
    public String config( ) {
        return this.localService.getAgentConfig().toString();
    }
	
}

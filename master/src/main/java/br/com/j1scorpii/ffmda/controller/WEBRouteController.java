package br.com.j1scorpii.ffmda.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.j1scorpii.ffmda.services.LocalService;

@Controller
public class WEBRouteController {

    @Value("${spring.application.name}")
    String appName;
    
    @Autowired private LocalService localService;

    @GetMapping("/")
    public String homePage(Model model) {
    	this.setGenericModel( model );
        return "index";
    }
    
    @GetMapping("/local")
    public String local(Model model) {
    	this.setGenericModel( model );
        return "local";
    }

    @GetMapping("/component")
    public String component( @RequestParam(value = "name", required = true) String name, Model model) {
    	this.setGenericModel( model );
    	model.addAttribute("componentName", name);
        return "component";
    }
    
    @GetMapping("/remote")
    public String remote(Model model) {
    	this.setGenericModel( model );
        return "remote";
    }

    private void setGenericModel( Model model ) {
   	
        model.addAttribute("appName", appName);
        model.addAttribute("walletAddress", localService.getWallet().getAddress() );
        model.addAttribute("systemReady", localService.amIReady() );
        model.addAttribute("walletBalance", localService.getMyWalletBalance() );
        
        // Inform to front if the stack is locked to edit
        JSONObject config = localService.getAgentConfig();
        model.addAttribute("stackIsLocked", config.getJSONObject("stackStatus").getBoolean("locked") );
        model.addAttribute("orgName", config.getString("orgName") );
        model.addAttribute("nodeName", config.getString("nodeName") );
        model.addAttribute("serverIpAddress", config.getString("ipAddress") );
        model.addAttribute("serverHostName", config.getString("hostName"));
        
    }
    
    
}

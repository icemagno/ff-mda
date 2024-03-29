package br.com.j1scorpii.ffmda.controller;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.j1scorpii.ffmda.services.LocalService;
import jakarta.annotation.PostConstruct;

@Controller
public class WEBRouteController {

    @Value("${spring.application.name}")
    String appName;
    
    private Map<String,String> componentNames = new HashMap<String,String>();
    
    @Autowired private LocalService localService;

    
    @PostConstruct
    private void init() {
    	componentNames.put("dataexchange", "Data Exchange");
    	componentNames.put("postgres", "PostgreSQL Database");
    	componentNames.put("ipfs", "IPFS Node");
    	componentNames.put("besu", "HyperLedger Besu Node");
    	componentNames.put("tokens", "FireFly Tokens");
    	componentNames.put("signer", "FireFly Signer");
    	componentNames.put("evmconnect", "FireFly EVM Connector");
    	componentNames.put("sandbox", "FireFly Sandbox");
    	componentNames.put("core", "HyperLedger FireFly Core");
    }
    
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
    	model.addAttribute("componentName", this.componentNames.get(name) );
    	model.addAttribute("componentShortName", name );
        return "components/" + name ;
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

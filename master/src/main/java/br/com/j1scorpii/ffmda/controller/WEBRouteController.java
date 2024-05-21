package br.com.j1scorpii.ffmda.controller;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import br.com.j1scorpii.ffmda.agent.RemoteAgent;
import br.com.j1scorpii.ffmda.services.LocalService;
import br.com.j1scorpii.ffmda.services.RemoteAgentService;
import jakarta.annotation.PostConstruct;

@Controller
public class WEBRouteController {

    @Value("${spring.application.name}")
    String appName;
    
    private Map<String,String> componentNames = new HashMap<String,String>();
    
    @Autowired private LocalService localService;
    @Autowired private RemoteAgentService remoteAgentService;
    
    @PostConstruct
    private void init() {
    	componentNames.put("dataexchange", "Data Exchange");
    	componentNames.put("postgresql", "PostgreSQL Database");
    	componentNames.put("ipfs", "IPFS Node");
    	componentNames.put("besu", "HyperLedger Besu");
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

    @GetMapping("/local/{name}")
    public String localComponent( @PathVariable String name, Model model) {
    	this.setGenericModel( model );
    	model.addAttribute("componentName", this.componentNames.get(name) );
    	model.addAttribute("componentShortName", name );

    	// Try to take the main config
    	try {
    		// In case of succes ( already have configured ) go to component page
    		String configured = localService.getMainConfig().getString("nodeName");
    		if( configured.length() > 1 ) return "local/" + name ;
    	} catch ( Exception e ) {	}
    	
    	// Failure. Go back to main page
    	return "local";
    }
    
    // All remote Agents / Register and delete an Agent
    @GetMapping("/remote")
    public String remote(Model model) {
    	this.setGenericModel( model );
    	
    	if( model.getAttribute("orgName") == null ) return "local"; 
    	
        return "remote";
    }

    // Remote Agent control panel
    @GetMapping("/remote/{agentId}")
    public String manageAgent( @PathVariable String agentId, Model model) {
    	model.addAttribute("agentId", agentId );
    	
    	RemoteAgent agent = this.remoteAgentService.getAgentById( agentId );
    	model.addAttribute("agent", agent );
    	
    	this.setGenericModel( model );
        return "remote/manage";
    }

    // Manage a remote component from an Agent
    @GetMapping("/remote/{agentId}/{name}")
    public String manageAgentComponent( @PathVariable String agentId, @PathVariable String name, Model model) {
    	this.setGenericModel( model );
    	model.addAttribute("componentName", this.componentNames.get(name) );
    	model.addAttribute("componentShortName", name );
    	model.addAttribute("agentId", agentId );
   		return "remote/" + name ;
    }
    
    private void setGenericModel( Model model ) {
   	
        model.addAttribute("appName", appName);
        model.addAttribute("walletAddress", localService.getWallet().getAddress() );
        model.addAttribute("systemReady", localService.amIReady() );
        model.addAttribute("walletBalance", localService.getMyWalletBalance() );
        
        // Inform to front if the stack is locked to edit
        JSONObject config = localService.getMainConfig();
        model.addAttribute("stackIsLocked", config.getJSONObject("stackStatus").getBoolean("locked") );
        model.addAttribute("orgName", config.getString("orgName") );
        model.addAttribute("nodeName", config.getString("nodeName") );
        model.addAttribute("serverIpAddress", config.getString("ipAddress") );
        model.addAttribute("serverHostName", config.getString("hostName"));
        
    }
    
    
}

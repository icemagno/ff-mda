package br.com.j1scorpii.ffmda.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
    }
    
    
}

package br.com.j1scorpii.ffmda.services;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class TipService {
	private Logger logger = LoggerFactory.getLogger( TipService.class );
	private JSONObject currentMainTip;
	
	@PostConstruct
	private void init() {
		logger.info("init");
		
		// Set initial tips to show the user what the system expects him to do
		this.currentMainTip = new JSONObject();
		this.currentMainTip.put("mainTip", "FireFly Supernode").put("mainSubTip", "Follow the tabs below in its order to start a FireFly Supernode.").put("show", true);
	}
	
	public void showMainTip() {
		this.currentMainTip.put("show", true);
	}
	
	public void hideMainTip() {
		this.currentMainTip.put("show", false);
	}
	
	public JSONObject getCurrentMainTip() {
		return this.currentMainTip;
	}	
	
}

package br.com.j1scorpii.ffmda.services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.dockerjava.transport.DockerHttpClient.Request;

import jakarta.annotation.PostConstruct;

@Service
public class NetworkManager {
	private Logger logger = LoggerFactory.getLogger( NetworkManager.class );

	
	@Autowired
	private DockerService dockerService;
	
	@PostConstruct
	private void init() {
		logger.info("init");
	}	
	
	public String createNetwork( String networkName ) {
		JSONObject body = new JSONObject();
		body.put("Name", networkName);
		return dockerService.getResponse( Request.Method.POST, "/networks/create", body );
	}

	public String deleteNetwork( String networkName ) {
		return dockerService.getResponse( Request.Method.DELETE,"/networks/" + networkName, null );
	}
	
	public boolean exists( String networkName ) {
		JSONArray networks = new JSONArray( list() );
		for( int x=0; x < networks.length(); x++ ) {
			if( networks.getJSONObject(x).getString("Name").toUpperCase().equals( networkName.toUpperCase() ) ) return true;
		}
		return false;
	}
	
	public String list( ) {
		return dockerService.getResponse( Request.Method.GET,"/networks", null );
	}

	public String connect( String networkName, String containerId ) {
		JSONObject body = new JSONObject();
		body.put("Container", containerId);
		return dockerService.getResponse( Request.Method.POST, "/networks/"+networkName+"/connect", body);  
	}
	

	public String disconnect( String networkName, String containerId ) {
		JSONObject body = new JSONObject();
		body.put("Container", containerId);
		return dockerService.getResponse( Request.Method.POST,"/networks/"+networkName+"/disconnect", body );
	}
	
	
}

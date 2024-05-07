package br.com.j1scorpii.ffmda.services;

import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

@Service
public class BESUService {
	private Logger logger = LoggerFactory.getLogger( BESUService.class );
	private RestTemplate rt;
	
	@PostConstruct
	private void init() {
		this.rt = new RestTemplate();
		logger.info("init");
	}

	public JSONObject getNodeID( ) {
		JSONObject res = new JSONObject ();
		try {
			JSONObject requestData = new JSONObject()
					.put("jsonrpc", "2.0")
					.put("id", 99)
					.put("params", new JSONArray() )
					.put("method", "net_enode");
			res = new JSONObject ( this.requestData( "http://besu:8545", requestData) );
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res; 
	}
	
	private String requestData( String endpoint, JSONObject payload ) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		RequestEntity<String> requestEntity = RequestEntity 
				.post( new URL( endpoint ).toURI() ) 
				.contentType( MediaType.APPLICATION_JSON ) 
				.body( payload.toString() ); 
		return rt.exchange(requestEntity, String.class ).getBody();		
	}	
	
	
}

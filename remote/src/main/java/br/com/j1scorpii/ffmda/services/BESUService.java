package br.com.j1scorpii.ffmda.services;

import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
	private String besuEnode;
	private final String COMPONENT_NAME = "besu";
	private String imageName;
	
	@Autowired private ImageManager imageManager;
	@Autowired private ContainerManager containerManager;	
	
	@PostConstruct
	private void init() {
		this.rt = new RestTemplate();
		logger.info("init");
		besuEnode = "";		
	}
	
	public String getBesuEnode() {
		return besuEnode;
	}

	// Get the image data
	public JSONObject imagePulled() {
		JSONObject result = new JSONObject();
		boolean exists = imageManager.exists(COMPONENT_NAME);
		result.put("exists", exists);
		if( exists ) {
			this.imageName = imageManager.getImageForComponent(COMPONENT_NAME);
			result.put("imageName", this.imageName );
		}
		return result;
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
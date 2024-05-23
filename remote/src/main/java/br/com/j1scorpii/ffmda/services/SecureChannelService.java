package br.com.j1scorpii.ffmda.services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class SecureChannelService {

	public JSONObject decrypt( JSONObject payload ) {
		return new JSONObject( decrypt( payload.toString() ) );
	}
	
	public JSONArray decrypt( JSONArray payload ) {
		return new JSONArray( decrypt( payload.toString() ) );
	}

	public String decrypt( String payload ) {
		return payload;
	}

	public JSONObject encrypt( JSONObject payload ) {
		return new JSONObject( encrypt( payload.toString() ) );
	}
	
	public JSONArray encrypt( JSONArray payload ) {
		return new JSONArray( encrypt( payload.toString() ) );
	}
	
	public String encrypt( String payload ) {
		return payload;
	}
	
}

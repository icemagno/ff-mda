package br.com.j1scorpii.ffmda.services;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataExchangeService {

	@Autowired private ImageManager imageManager;
	
	private final String COMPONENT_NAME = "dataexchange";
	
	public String imagePulled() {
		JSONObject result = new JSONObject();
		boolean exists = imageManager.exists(COMPONENT_NAME);
		result.put("exists", exists);

		
		if( exists ) {
			result.put("imageName", imageManager.getImageForComponent(COMPONENT_NAME) ); 
		}
		
		
		return result.toString();
	}
	
}

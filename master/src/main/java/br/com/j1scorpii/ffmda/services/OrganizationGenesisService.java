package br.com.j1scorpii.ffmda.services;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrganizationGenesisService {
	@Autowired private LocalService localService;
	@Autowired private BESUService besuService;
	

	// I need to do this because I need the Org data ( IP address )
	// to generate validators because the ENODE address and static nodes.
	public JSONObject saveOrgData(String data) throws Exception {
		JSONObject orgData = localService.saveOrgData(data);
		besuService.createValidatorNodes( );
		return orgData;
	}
	
	
}

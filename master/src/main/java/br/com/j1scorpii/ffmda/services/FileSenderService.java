package br.com.j1scorpii.ffmda.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.j1scorpii.ffmda.agent.RemoteAgent;
import br.com.j1scorpii.ffmda.util.FFMDAProtocol;

@Service
public class FileSenderService {
	
	@Autowired private SecureChannelService secChannel;

	public void sendFile( String component, RemoteAgent target, String fileName ) {
		try {
			String fileContent = readFile( fileName );
			
			JSONObject payload = new JSONObject()
					.put("protocol", FFMDAProtocol.FILE.toString() )
					.put("fileContent", fileContent )
					.put("component", component)
					.put("fileName", fileName );
			target.send( secChannel.encrypt( payload ) );
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String readFile(String path ) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, StandardCharsets.UTF_8 );
	}
	
}

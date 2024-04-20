package br.com.j1scorpii.ffmda.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShellRunnerService {
	private Logger logger = LoggerFactory.getLogger( ShellRunnerService.class );
	
	@Autowired private SimpMessagingTemplate messagingTemplate;	
	
	// String[] command = { "./copyfiles.sh", this.w, this.s, this.e, this.n, sourceSSHIPAddress, sourceSSHPassword, sourceSSHUserName, from, to };
	
	public void runShell( String[] command, String callbackChannel ) {

		try {
			String[] environments = null;
		    Process process = Runtime.getRuntime().exec(command, environments, new File( "/scripts" ) ); 
		    
		    BufferedReader stdInput = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
		    BufferedReader stdError = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );		    

		    String s = null;
		    while ((s = stdInput.readLine()) != null) {
		        logger.info("[ SHELL ] | " + s );
		        messagingTemplate.convertAndSend( callbackChannel, new JSONObject( ).put("std", "log").put("shell", s).toString() );
		    }

		    while ((s = stdError.readLine()) != null) {
		        logger.error("[ SHELL ] | " + s );
		        messagingTemplate.convertAndSend( callbackChannel, new JSONObject( ).put("std", "error").put("shell", s).toString() );
		    }			    
		    process.waitFor();
		} catch ( Exception e ) {
			e.printStackTrace();
		}		
		
		
	}
	
}

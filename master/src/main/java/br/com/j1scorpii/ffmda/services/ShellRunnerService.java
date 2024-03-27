package br.com.j1scorpii.ffmda.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ShellRunnerService {
	private Logger logger = LoggerFactory.getLogger( ShellRunnerService.class );
	
	// String[] command = { "./copyfiles.sh", this.w, this.s, this.e, this.n, sourceSSHIPAddress, sourceSSHPassword, sourceSSHUserName, from, to };
	
	public void runShell( String[] command ) {

		try {
			String[] environments = null;
		    Process process = Runtime.getRuntime().exec(command, environments, new File( "/scripts" ) ); 
		    
		    BufferedReader stdInput = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
		    BufferedReader stdError = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );		    

		    String s = null;
		    while ((s = stdInput.readLine()) != null) {
		        logger.info("[ SHELL ] | " + s );
		    }

		    while ((s = stdError.readLine()) != null) {
		        logger.error("[ SHELL ] | " + s );
		    }			    
		    process.waitFor();
		} catch ( Exception e ) {
			e.printStackTrace();
		}		
		
		
	}
	
}

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
	
	
	public void runShell( String scriptName, String[] parameters ) {

		try {
			String[] environments = null;
			File workdir = new File( "/home/" );
		    Process process = Runtime.getRuntime().exec(parameters, environments, workdir); 
		    
		    BufferedReader stdInput = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
		    BufferedReader stdError = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );		    

		    String s = null;
		    while ((s = stdInput.readLine()) != null) {
		        logger.info("  ["+scriptName+"] > " + s );
		    }

		    while ((s = stdError.readLine()) != null) {
		        logger.error("  ["+scriptName+"] > " + s );
		    }			    
		    process.waitFor();
		} catch ( Exception e ) {
			e.printStackTrace();
		}		
		
		
	}
	
	
	public void runShell( String scriptName ) {
		String[] command = { "./" + scriptName };
		runShell( scriptName, command );
	}
	
	
}

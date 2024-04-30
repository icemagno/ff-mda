package br.com.j1scorpii.ffmda.util;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EtcHosts {
	private Logger logger = LoggerFactory.getLogger( EtcHosts.class );
	private Map<String,String> hosts = new HashMap<String,String>();
	private String backupFile;
	
	public EtcHosts( String localDataFolder ) {
		this.backupFile = localDataFolder + "/hosts";
		// Restore our copy of /etc/hosts because Docker always replace its own at restart.
		if ( new File( this.backupFile ).exists() ) giveMyHostsBackPlease();
	}
	
	public void addIfNotExists( String ipAddress, String hostName ) {
		if( !hosts.containsKey( ipAddress ) || !hosts.get(ipAddress).equals(hostName) ) {
			hosts.put( ipAddress, hostName );
			save();
		} 
	}
	
	private void read( String fromWhere ) {
		try {
			Scanner s = new Scanner(new File( fromWhere ));
			while ( s.hasNextLine() ){
				String line = s.nextLine();
				if( !line.contains("FF-MDA") ) {
					String[] keyVal = line.split(" ");
					hosts.put(keyVal[0], keyVal[1]);
				}
			}
			s.close();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
	private void giveMyHostsBackPlease() {
		read( this.backupFile );
		save("/etc/hosts");
	}
	
	public void save() {
		// Save a backup too because Docker will override the original file
		// as soon as it restarts.
		// We will override again with our own copy. Cheater!
		save("/etc/hosts");
		save( this.backupFile );
	}
	
	public void print() {
		for (Map.Entry<String, String> entry : hosts.entrySet()) {
		    String key = entry.getKey();
		    String value = entry.getValue();
		    logger.info( key + " ---> " + value );
		}
	}
	
	private void save( String toWhere ) {
		try {
			FileWriter writer = new FileWriter( toWhere );
			writer.write( "#  FF-MDA controls this file. Do not edit! " + System.lineSeparator() );
			for (Map.Entry<String, String> entry : hosts.entrySet()) {
			    String key = entry.getKey();
			    String value = entry.getValue();
			    writer.write( key + " " + value + System.lineSeparator());
			}
			writer.close();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	public Map<String, String> getHosts() {
		return hosts;
	}
}

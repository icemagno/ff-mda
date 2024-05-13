package br.com.j1scorpii.ffmda.services;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import br.com.j1scorpii.ffmda.util.IFireFlyComponent;
import jakarta.annotation.PostConstruct;

@Service
@Order(value = 1)
@EnableScheduling
public class EtcHostsService {
	private Logger logger = LoggerFactory.getLogger( EtcHostsService.class );
	private Map<String,String> hosts = new HashMap<String,String>();
	private String backupFile;
	private List<IFireFlyComponent> components = new ArrayList<IFireFlyComponent>();
	
	@Value("${ffmda.local.data.folder}")
	private String localDataFolder;	
	
	@PostConstruct
	private void init() {
		this.backupFile = localDataFolder + "/hosts";
		// Restore our copy of /etc/hosts because Docker always replace its own at restart.
		if ( new File( this.backupFile ).exists() ) { 
			restoreTo("/etc");
		} else {
			addIfNotExists("127.0.0.1", "localhost");
		}
	}
	
	public void addIfNotExists( String ipAddress, String hostName ) {
		if( !hosts.containsKey( ipAddress ) || !hosts.get(ipAddress).equals(hostName) ) {
			logger.info("adding " + ipAddress + " as " + hostName );
			hosts.put( ipAddress, hostName );
			save();
		} 
	}
	
	@Scheduled(fixedDelay = 5000 )
	private void updateCompnentsHosts() {
		this.components.parallelStream().forEach( (component)->{
			restoreTo( component.getComponentDataFolder() );
		});
	}
	
	public void register( IFireFlyComponent component ) {
		this.components.add(component);
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
	
	private void save() {
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

	private void restoreTo(String targetFolder ) {
		read( this.backupFile );
		save( targetFolder + "/hosts");	
	}
	
}

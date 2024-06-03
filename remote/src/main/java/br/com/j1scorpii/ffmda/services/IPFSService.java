package br.com.j1scorpii.ffmda.services;

import java.io.File;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class IPFSService {
	private Logger logger = LoggerFactory.getLogger( IPFSService.class );
	
	private final String COMPONENT_NAME = "ipfs";
	
	@Value("${ffmda.data.folder}")
	private String localDataFolder;		
	
	private String componentDataFolder;
	private String swarmKeyFile;
	private String stagingFolder;
	private String dataFolder;
	private String imageName;
	private String configFile;
	private JSONObject identity = new JSONObject();
	private JSONObject config;
	private String swarmKey;	
	
	@PostConstruct
	private void init() {
		// Configure folders
		this.componentDataFolder = localDataFolder + "/" + COMPONENT_NAME;
		this.stagingFolder = this.componentDataFolder + "/staging";
		this.dataFolder = this.componentDataFolder + "/data";
		this.configFile = this.dataFolder + "/config";
		this.swarmKeyFile = this.dataFolder + "/swarm.key";
		logger.info("init " + this.componentDataFolder );
		new File( this.dataFolder ).mkdirs();
		new File( this.stagingFolder ).mkdirs();
	}

	public String getComponentDataFolder() {
		return componentDataFolder;
	}
	
}

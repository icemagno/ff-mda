package br.com.j1scorpii.ffmda.services;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class DataExchangeService {
	private Logger logger = LoggerFactory.getLogger( DataExchangeService.class );
	
	private final String COMPONENT_NAME = "dataexchange";
	
	@Value("${ffmda.data.folder}")
	private String localDataFolder;	
	
	private String componentDataFolder;
	private String peersFolder;
	private String configFile;
	private String imageName;
	private String pemCer;
	private String pemKey;	
	
	@PostConstruct
	private void init() {
		this.componentDataFolder  = localDataFolder + "/" + COMPONENT_NAME;
		this.peersFolder = this.componentDataFolder + "/peer-certs";
		this.configFile = this.componentDataFolder + "/config.json";
		this.pemCer = this.componentDataFolder + "/cert.pem";
		this.pemKey = this.componentDataFolder + "/key.pem";
		new File( this.peersFolder ).mkdirs();
		logger.info("init " + this.componentDataFolder );
	}	
	
	public String getComponentDataFolder() {
		return componentDataFolder;
	}
	
}

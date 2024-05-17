package br.com.j1scorpii.ffmda.services;

import java.util.Iterator;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import br.com.j1scorpii.ffmda.util.ImageDownloader;
import jakarta.annotation.PostConstruct;

@Service
@Order(value = 3)
public class ImageDownloaderService {
	private Logger logger = LoggerFactory.getLogger( ImageDownloaderService.class );
	
	@Autowired private ImageManager imageManager;
	
	@PostConstruct
	private void init() {
		logger.info("init 3");
		JSONObject manifest = imageManager.getManifest();

		logger.info("will pull all images from manifest file...");
		Iterator<String> components = manifest.keys();
		while( components.hasNext()) {
		    String component = components.next();
		    String imageName = manifest.getString(component);
		    ImageDownloader id = new ImageDownloader( component, imageName, imageManager );
		    new Thread( id ).start();
		}
		
	}
	
}

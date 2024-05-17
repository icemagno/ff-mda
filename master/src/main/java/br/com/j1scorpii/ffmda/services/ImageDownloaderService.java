package br.com.j1scorpii.ffmda.services;

import java.util.Iterator;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.j1scorpii.ffmda.util.ImageDownloader;
import jakarta.annotation.PostConstruct;

@Service
public class ImageDownloaderService {
	private Logger logger = LoggerFactory.getLogger( ImageDownloaderService.class );
	
	@Autowired private ImageManager imageManager;
	
	// This will start a thread to pull every image on manifest
	// in parallel.
	
	@PostConstruct
	private void init() {
		logger.info("init");
		pullAll();
	}

	private void pullAll() {
		JSONObject manifest = imageManager.getManifest();

		logger.info("will pull all images from manifest file...");
		Iterator<String> components = manifest.keys();
		
		// For all images on manifest file...
		while( components.hasNext()) {
		    String componentName = components.next();
		    String imageName = manifest.getString(componentName);
		    
		    // Already here?
		    boolean found = false;
			for( String image : imageManager.listAvailableImages() ) {
				if( image.toUpperCase().contains( imageName.toUpperCase() ) ) {
					logger.info("  > found image " + image + " for " + componentName + ". Skip. " );
					found = true;
				}
			}		    
		    
		    if( !found ) {
				logger.info("  > image " + imageName + " for " + componentName + " not found. Pulling... ");
			    ImageDownloader id = new ImageDownloader( componentName, imageName, imageManager );
			    new Thread( id ).start();
		    }
		}
			
	}
}

package br.com.j1scorpii.ffmda.services;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import br.com.j1scorpii.ffmda.util.IObservable;
import br.com.j1scorpii.ffmda.util.ImageDownloader;
import br.com.j1scorpii.ffmda.util.ImageDownloaderStatus;
import jakarta.annotation.PostConstruct;

@Service
@EnableScheduling
public class ImageDownloaderService implements IObservable {
	private Logger logger = LoggerFactory.getLogger( ImageDownloaderService.class );
	private Map<ImageDownloader,Boolean> workers = new HashMap<ImageDownloader,Boolean>();
	
	@Autowired private ImageManager imageManager;
	
	// This will start a thread to pull every image on manifest
	// in parallel.
	@PostConstruct
	private void init() {
		logger.info("init");
	}
	
	public void pull( String componentName, IObservable taskOwner ) {
		JSONObject manifest = imageManager.getManifest();
	    String imageName = manifest.getString(componentName);
	    boolean found = false;
		for( String image : imageManager.listAvailableImages() ) {
			if( image.toUpperCase().contains( imageName.toUpperCase() ) ) {
				logger.info("  > found image " + image + " for " + componentName + ". Skip. " );
				// Will notify the task owner anyway even if we already have the image here
				taskOwner.notify(componentName);
				found = true;
			}
		}		    
	    if( !found ) {
			logger.info("  > image " + imageName + " for " + componentName + " not found. Pulling... ");
		    ImageDownloader id = new ImageDownloader( componentName, imageName, imageManager, taskOwner );
		    this.workers.put(id, true);
		    new Thread( id ).start();
	    }
	}
	
	@Scheduled( fixedDelay = 3000 )
	private void showStatus() {
		for (Map.Entry<ImageDownloader, Boolean> entry : workers.entrySet()) {
			ImageDownloader worker = entry.getKey();
		    Boolean working = entry.getValue();
		    if( working ) worker.showStatus();
		    if( worker.getStatus().equals( ImageDownloaderStatus.FINISHED) ) entry.setValue(false);
		}		
	}

	public void pullAll( ) {
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
			    ImageDownloader id = new ImageDownloader( componentName, imageName, imageManager, this );
			    this.workers.put(id, true);
			    new Thread( id ).start();
		    }
		}
	}

	// ImageDownloader will trigger when pulling is done
	@Override
	public synchronized void notify( String componentName ) {
		// TODO Auto-generated method stub
	}
}

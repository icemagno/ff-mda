package br.com.j1scorpii.ffmda.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.j1scorpii.ffmda.services.ImageManager;

public class ImageDownloader implements Runnable {
	private Logger logger = LoggerFactory.getLogger( ImageDownloader.class );

	private ImageManager imageManager;
	private String imageName;
	private String componentName;
	private boolean working = false;
	private IObservable observable;
	
	public ImageDownloader( String componentName, String imageName, ImageManager imageManager, IObservable observable ) {
		this.imageManager = imageManager;
		this.componentName = componentName;
		this.imageName = imageName;
		this.observable = observable;
	}
	
	public boolean isWorking() {
		return working;
	}
	
	@Override
	public void run() {
		logger.info( "  > [ INIT ]  downloading " + imageName + " for " + componentName );
		working = true;
		imageManager.pullImage( componentName, true );
		logger.info( "  > [ DONE ]  downloading " + imageName + " for " + componentName );
		working = false;
		if( this.observable != null ) this.observable.notify();
	}

}

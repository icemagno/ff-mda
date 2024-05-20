package br.com.j1scorpii.ffmda.util;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.j1scorpii.ffmda.services.ImageManager;

public class ImageDownloader implements Runnable {
	private Logger logger = LoggerFactory.getLogger( ImageDownloader.class );

	private ImageManager imageManager;
	private String imageName;
	private String componentName;
	private ImageDownloaderStatus status = ImageDownloaderStatus.IDLE;
	private IObservable observable;
	
	public ImageDownloader( String componentName, String imageName, ImageManager imageManager, IObservable observable ) {
		this.imageManager = imageManager;
		this.componentName = componentName;
		this.imageName = imageName;
		this.observable = observable;
	}
	
	public void showStatus() {
		logger.info( componentName + " : " + status.toString() );
	}

	public ImageDownloaderStatus getStatus() {
		return status;
	}
	
	public void setStatus(ImageDownloaderStatus status) {
		this.status = status;
	}
	
	@Override
	public void run() {
		logger.info( "  > [ INIT ]  downloading " + imageName + " for " + componentName );
		status = ImageDownloaderStatus.PULLING;
		JSONObject res = new JSONObject( imageManager.pullImage( componentName, false ) );
		logger.info( "  > [ INFO ]  " + componentName + ": " + res.getString("response")  );
		logger.info( "  > [ DONE ]  downloading " + imageName + " for " + componentName );
		status = ImageDownloaderStatus.FINISHED;
		if( this.observable != null ) this.observable.notify( componentName );
	}

}

package br.com.j1scorpii.ffmda.services;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.dockerjava.transport.DockerHttpClient.Request;

import jakarta.annotation.PostConstruct;

@Service
public class ImageManager {
	private Logger logger = LoggerFactory.getLogger( ImageManager.class );
	private JSONArray images;
	private JSONObject manifest;
	private Map<String,Boolean> inProcessPulling = new HashMap<String,Boolean>();
	
	@Value("${ffmda.local.data.folder}")
	private String localDataFolder;		
	
	@Value("${spring.profiles.active}")
	private String activeProfile;
	
	@Autowired
	private DockerService dockerService;
	
	@PostConstruct
	private void init() {
		logger.info("init");
		updateImageCache();
		readManifest();
	}
	
	public JSONObject getManifest() {
		return this.manifest;
	}
	
	public String getImageForComponent( String componentName ) {
		if( !manifest.has(componentName) ) return null;
		return manifest.getString(componentName);
	}

	// Search docker images to see if the image from the component exists
	// I will get the image name for that component from the manifest file
	public boolean exists( String componentName ) {
		logger.info("checking if I have an image for " + componentName );
		try {
			updateImageCache();
			String imageAndTag = getImageForComponent(componentName);
			if( imageAndTag == null ) return false;
			String imageName = imageAndTag.split(":")[0];
			for( String image : this.listAvailableImages() ) {
				if( image.toUpperCase().contains( imageName.toUpperCase() ) ) {
					logger.info("found image " + image + " for " + componentName );
					return true;
				}
			}
		} catch ( Exception e ) { }
		logger.info("no image found for "  + componentName );
		return false;
	}
	
	public void updateImageCache() {
		logger.info("updating image cache ... " );
		this.images = new JSONArray( this.getImages() );
		logger.info("found " + this.images.length() + " images ");
	}
	
	
	private String doPull( String imageName, String callbackChannel, String componentName ) {
		
		logger.info("pulling image " + imageName + "... " );

		inProcessPulling.put(componentName, true);
		System.out.println("X_SET " + componentName + " to TRUE");
		
		dockerService.pullImage( imageName, callbackChannel );
		logger.info( imageName + " pull done. " );

		System.out.println("X_SET " + componentName + " to FALSE");
		inProcessPulling.put(componentName, false);		
		
		return new JSONObject().put("response", "Done.").toString();
	}
	
	private boolean isPulling( String componentName ) {
		boolean containsKey = this.inProcessPulling.containsKey(componentName);
		boolean inProcessPulling = false;
		if( containsKey ) {
			inProcessPulling = this.inProcessPulling.get( componentName );
		} else {
			//
		}
		System.out.println("QUERY 2 " + componentName + " =  CK: " + containsKey + "    IPP: " + inProcessPulling );
		return ( containsKey && inProcessPulling );
	}
	
	public String pullImage( String componentName, boolean evenIfExists ) {
		String callbackChannel = "/docker/"+componentName+"/pull";
		String imageName = getImageForComponent( componentName );
		if ( isPulling(componentName) ) return new JSONObject().put("response", "Image '" + imageName + "' already pulling.").toString();
		if( imageName == null ) {
			return new JSONObject().put("response", "Component '" + componentName + "' has no image entry in manifest file.").toString();
		}
		if( evenIfExists ) {
			return doPull( imageName, callbackChannel, componentName );
		} else if( !exists(imageName) ) return doPull( imageName, callbackChannel, componentName );
		return new JSONObject().put("response", "Image '" + imageName + "' already exists.").toString();
	}
	
	public String deleteImage( String imageName, boolean force ) {
		return dockerService.getResponse( Request.Method.DELETE, "/images/"+imageName+"?force="+force, null );
	}

	public String getImages() {
		// If active profile is DEV then I will mock the image list. I'm on Windows and have no Docker socket to test.
		if( this.activeProfile.equals("dev") ) { 
			try {	
				byte[] encoded = Files.readAllBytes(Paths.get( this.localDataFolder + "/images-mock.json" ));
				return new String(encoded, StandardCharsets.UTF_8 );
			}catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		// The game is for real. Read images from Docker.
		return dockerService.getResponse( Request.Method.GET, "/images/json?all=false&digests=true", null );
	}
	
	public List<String> listAvailableImages() {
		List<String> result = new ArrayList<String>();
		updateImageCache();
		for( int x=0; x < images.length(); x++ ) {
			JSONObject img = images.getJSONObject(x);

			JSONArray nameList = img.optJSONArray("RepoTags");
			if( nameList != null ) {
				if( nameList.length() > 0 ) {
					result.add( nameList.getString(0) );
				} else {
					nameList = img.getJSONArray("RepoDigests");
					if( nameList.length() > 0 ) {
						result.add( nameList.getString(0).split("@")[0] );
					}
				}
			}
			
		}
		return result;
	}
	
	private void readManifest() {
		try {	
			byte[] encoded = Files.readAllBytes(Paths.get( this.localDataFolder + "/manifest.json" ));
			this.manifest = new JSONObject( new String(encoded, StandardCharsets.UTF_8 ) );
		}catch (Exception e) {
			e.printStackTrace();
		}		
	}
		
	
}

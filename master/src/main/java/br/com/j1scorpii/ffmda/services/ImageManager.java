package br.com.j1scorpii.ffmda.services;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
	
	public String getImageForComponent( String componentName ) {
		if( !manifest.has(componentName) ) return null;
		return manifest.getString(componentName);
	}

	// Search docker images to see if the image from the component exists
	// I will get the image name for that component from the manifest file
	public boolean exists( String componentName ) {
		updateImageCache();
		String imageAndTag = getImageForComponent(componentName);
		if( imageAndTag == null ) return false;
		String imageName = imageAndTag.split(":")[0];
		for( String image : this.listAvailableImages() ) {
			if( image.toUpperCase().contains( imageName.toUpperCase() ) ) return true;
		}
		return false;
	}
	
	public void updateImageCache() {
		this.images = new JSONArray( this.getImages() );
	}
	
	
	private String doPull( String imageName, String callbackChannel ) {
		dockerService.pullImage( imageName, callbackChannel );
		return "requested";
	}
	public String pullImage( String componentName, boolean evenIfExists ) {
		String callbackChannel = "/docker/"+componentName+"/pull";
		String imageName = getImageForComponent( componentName );
		if( imageName == null ) {
			return new JSONObject().put("response", "Component '" + componentName + "' has no image entry in manifest file.").toString();
		}
		if( evenIfExists ) {
			return doPull( imageName, callbackChannel );
		} else if( !exists(imageName) ) return doPull( imageName, callbackChannel );
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
		for( int x=0; x < images.length(); x++ ) {
			JSONObject img = images.getJSONObject(x);
			JSONArray nameList = img.getJSONArray("RepoTags");
			
			if( nameList.length() > 0 ) {
				result.add( nameList.getString(0) );
			} else {
				nameList = img.getJSONArray("RepoDigests");
				if( nameList.length() > 0 ) {
					result.add( nameList.getString(0).split("@")[0] );
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

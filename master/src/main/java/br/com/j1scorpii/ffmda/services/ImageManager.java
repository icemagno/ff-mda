package br.com.j1scorpii.ffmda.services;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.dockerjava.transport.DockerHttpClient.Request;

import jakarta.annotation.PostConstruct;

@Service
public class ImageManager {
	private Logger logger = LoggerFactory.getLogger( ImageManager.class );
	private JSONArray images;
	
	@Autowired
	private DockerService dockerService;
	
	@PostConstruct
	private void init() {
		logger.info("init");
		updateImageCache();
	}
	
	public boolean exists( String imageName ) {
		for( String image : this.listAvailableImages() ) {
			if( image.toUpperCase().equals( imageName.toUpperCase() ) ) return true;
		}
		return false;
	}
	
	public void updateImageCache() {
		this.images = new JSONArray( this.getImages() );
	}
	
	
	private String doPull( String imageName ) {
		return dockerService.getResponse( Request.Method.POST, "/images/create?fromImage="+imageName, null );
	}
	public String pullImage( String imageName, boolean evenIfExists ) {
		if( evenIfExists ) {
			return doPull(imageName);
		} else if( !exists(imageName) ) return doPull(imageName);
		return new JSONObject().put("response", "Image " + imageName + " already exists.").toString();
	}
	
	public String deleteImage( String imageName, boolean force ) {
		return dockerService.getResponse( Request.Method.DELETE, "/images/"+imageName+"?force="+force, null );
	}

	public String getImages() {
		return dockerService.getResponse( Request.Method.GET, "/images/json?all=false&digests=true", null );
	}
	
	public List<String> listAvailableImages() {
		List<String> result = new ArrayList<String>();
		for( int x=0; x < images.length(); x++ ) {
			JSONObject img = images.getJSONObject(x);
			String name = null;
			try { 
				name = img.getJSONArray("RepoTags").getString(0);
			} catch( Exception e ) {
				name = img.getJSONArray("RepoDigests").getString(0).split("@")[0];
			}
			result.add( name.trim() );
		}
		return result;
	}
		
	
}

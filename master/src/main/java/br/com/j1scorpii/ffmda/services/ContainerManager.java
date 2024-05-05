package br.com.j1scorpii.ffmda.services;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.dockerjava.transport.DockerHttpClient.Request;

import br.com.j1scorpii.ffmda.enums.ContainerStatus;
import jakarta.annotation.PostConstruct;

@Service
public class ContainerManager {
	private Logger logger = LoggerFactory.getLogger( ContainerManager.class );
	private JSONArray containers;
	
	@Value("${ffmda.local.data.folder}")
	private String localDataFolder;	
	
	@Autowired private DockerService dockerService;
	@Autowired private NetworkManager networkManager;
	
	@Value("${spring.profiles.active}")
	private String activeProfile;	
	
	@PostConstruct
	private void init() {
		logger.info("init");
		this.updateContainers();
	}

	public boolean exists( String componentName ) {
		for( int x=0; x < this.containers.length(); x++  ) {
			JSONObject container = this.containers.getJSONObject(x);
			String name = container.getJSONArray("Names").getString(0).replace("/", "");
			if( name.toUpperCase().equals( componentName.toUpperCase()) ) return true;
		}
		return false;
	}
	
	public JSONObject getContainer(String componentName) {
		updateContainers();
		for( int x=0; x < this.containers.length(); x++  ) {
			JSONObject container = this.containers.getJSONObject(x);
			String name = container.getJSONArray("Names").getString(0).replace("/", "");
			if( name.toUpperCase().equals( componentName.toUpperCase()) ) return container;
		}
		return new JSONObject();
	}	
	
	public void updateContainers() {
		this.containers = new JSONArray( getContainers() );
	}
	
	public String create( JSONObject container ) {
		String name = container.getString("name");
		String fromImage = container.getString("image");
		logger.info("Creating container " + name + " based on " + fromImage + " image.");
		
		String network = container.getString("connectToNetwork");
		
		JSONObject hostConfig = new JSONObject();
		if( container.has("volumes") ) 	hostConfig.put("Binds", container.getJSONArray("volumes") );
		if( container.has("restart") ) 	hostConfig.put("RestartPolicy", new JSONObject().put("Name", container.getString("restart") )  );
		if( container.has("hosts") )  	hostConfig.put("ExtraHosts", container.getJSONArray("hosts") );

		JSONObject body = new JSONObject();
		if( container.has("args") )		{
			body.put("Cmd", container.getJSONArray("args") );
		}
		
		if( container.has("ports") ) {
			JSONObject portBindings = new JSONObject();
			JSONObject exposedPorts = new JSONObject();

			JSONObject thePorts = container.getJSONObject("ports");
			Iterator<String> keys = thePorts.keys();
			

			while(keys.hasNext()) {
				JSONArray mappedPort = new JSONArray();
			    String key = keys.next();
			    String value = thePorts.getString(key);
			    
				mappedPort.put( new JSONObject().put("HostPort", key ) );
				portBindings.put( value, mappedPort );
				exposedPorts.put( value, new JSONObject() );			

			}
			
			hostConfig.put("PortBindings", portBindings );			
			body.put("ExposedPorts", exposedPorts );
		}
		
		body.put("Hostname", name);
		
		if( container.has("environments")) {
			JSONArray envs = container.getJSONArray("environments");
			if( envs.length() > 0 ) body.put("Env", envs );
		}
		body.put("Image", fromImage);
		body.put("HostConfig", hostConfig);
		body.put("Tty",true);

		JSONObject result = new JSONObject( this.dockerService.getResponse( Request.Method.POST, "/containers/create?name="+name, body ) );
		networkManager.connect( network, name );
				
		startContainer( name );
		
		return result.toString();
	}
	
	public ContainerStatus getStatus( String containerId ) {
		ContainerStatus status = ContainerStatus.NOT_FOUND;
		JSONObject stats = new JSONObject( getContainerStats( containerId ) );
		if( stats.has("precpu_stats") ) {
			JSONObject preCpuStats = stats.getJSONObject("precpu_stats");
			if( preCpuStats.has("online_cpus") ) 
				status = ContainerStatus.RUNNING;
			else 
				status = ContainerStatus.STOPPED; 
		} 
		return status;
	}
	
	public String startContainer( String containerId ) {
		logger.info("Starting component " + containerId );
		return dockerService.getResponse( Request.Method.POST, "/containers/"+containerId+"/start", null );
	}
	
	public String getLog( String containerId ) {
		String log = dockerService.getResponse( Request.Method.GET, "/containers/"+containerId+"/logs?stdout=true&stderr=true&tail=true", null );
		// Remove color codes from log to not mess with my interface!
		return new JSONObject().put("result", log.replaceAll("\u001B\\[[;\\d]*m", "") ).toString();		
	}

	public String deleteContainer( String containerId, boolean force, boolean removeVolumes ) {
		return dockerService.getResponse( Request.Method.DELETE, "/containers/"+containerId+"?force="+force+"&v="+removeVolumes, null );
	}
	
	public String killContainer( String containerId ) {
		return dockerService.getResponse( Request.Method.POST, "/containers/"+containerId+"/kill", null );
	}

	public String reStartContainer( String containerId ) {
		logger.info("Reiniciando container " + containerId );
		String result = dockerService.getResponse( Request.Method.POST, "/containers/"+containerId+"/restart", null );
		return new JSONObject().put("result", result ).toString();
	}

	public String stopContainer( String containerId ) {
		logger.info("Stopping component " + containerId );
		String result =  dockerService.getResponse( Request.Method.POST, "/containers/"+containerId+"/stop", null );
		return new JSONObject().put("result", result ).toString();
	}
	
	public String getContainerStats( String containerId ) {
		return dockerService.getResponse( Request.Method.GET, "/containers/"+containerId+"/stats?stream=false", null );
	}

	public String getContainers() {
		if( this.activeProfile.equals("dev") ) {
			try {	
				byte[] encoded = Files.readAllBytes(Paths.get( this.localDataFolder + "/containers-mock.json" ));
				return new String(encoded, StandardCharsets.UTF_8 );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return dockerService.getResponse( Request.Method.GET, "/containers/json?all=true&size=true", null );
	}

	public void stopAllButMe(String myName) {
		this.updateContainers();
		logger.info("Stopping all components except " + myName + "..." );
		for( int x=0; x < this.containers.length(); x++  ) {
			JSONObject container = this.containers.getJSONObject(x);
			//String id = container.getString("Id");
			String state = container.getString("State");
			String name = container.getJSONArray("Names").getString(0).replace("/", "");
			if( !name.toUpperCase().equals( myName.toUpperCase() ) ) {
				if( state.equals("running") ) {
					logger.info("  > Stopping '" + name + "' ...");
					stopContainer( name );
				}
			}
		}
	}

	public String exec(String containerName, String[] command) {
		return this.dockerService.execute(containerName, command );
	}

	
	public void executeAndRemoveContainer(String imageName, String[] command, String volumeHost, String volumeContainer) {
		this.dockerService.executeAndRemoveContainer( imageName, command, volumeHost, volumeContainer );
	}

}

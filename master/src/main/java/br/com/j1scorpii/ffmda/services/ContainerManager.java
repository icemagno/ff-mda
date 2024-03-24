package br.com.j1scorpii.ffmda.services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.dockerjava.transport.DockerHttpClient.Request;

import br.com.j1scorpii.ffmda.enums.ContainerStatus;
import jakarta.annotation.PostConstruct;

@Service
public class ContainerManager {
	private Logger logger = LoggerFactory.getLogger( ContainerManager.class );
	private JSONArray containers;
	
	@Autowired
	private DockerService dockerService;
	
	
	@PostConstruct
	private void init() {
		logger.info("init");
		//this.updateContainers();
	}

	public void updateContainers() {
		this.containers = new JSONArray( getContainers() );
	}
	
	public String create( JSONObject container ) {
		String name = container.getString("name");
		int port = container.getInt("port");
		int internalPort = container.getInt("internalPort");
		String fromImage = container.getString("image");
		String restart = container.getString("restart");
		
		logger.info("Creating container " + name + " based on " + fromImage + " image.");
		
		JSONObject hostConfig = new JSONObject();
		JSONObject portBindings = new JSONObject();

		if( internalPort > 0) {
			JSONArray mappedPort = new JSONArray();
			mappedPort.put( new JSONObject().put("HostPort", String.valueOf(port) ) );
			portBindings.put( internalPort + "/tcp", mappedPort);
			hostConfig.put("PortBindings", portBindings );
		}
		
		if( container.has("volumes") ) {
			JSONArray volumes = container.getJSONArray("volumes");
			hostConfig.put("Binds", volumes );
		}

		if( container.has("hosts") ) {
			JSONArray addHosts = container.getJSONArray("hosts");
			hostConfig.put("ExtraHosts", addHosts );
		}		
	
		hostConfig.put("RestartPolicy", new JSONObject().put("Name", restart)  );

		JSONObject exposedPorts = new JSONObject();
		exposedPorts.put( port + "/tcp", new JSONObject() );		
		
		JSONObject body = new JSONObject();
		body.put("Hostname", name);
		
		if( container.has("environments")) {
			JSONArray envs = container.getJSONArray("environments");
			body.put("Env", envs);
		}
		
		body.put("Image", fromImage);
		body.put("HostConfig", hostConfig);
		body.put("ExposedPorts", exposedPorts);
		body.put("Tty",true);

		return this.dockerService.getResponse( Request.Method.POST, "/containers/create?name="+name, body );
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
		logger.info("Staring component " + containerId );
		return dockerService.getResponse( Request.Method.POST, "/containers/"+containerId+"/start", null );
	}
	
	public String getLog( String containerId, String tail ) {
		return dockerService.getResponse( Request.Method.GET, "/containers/"+containerId+"/logs?stdout=true&stderr=true&tail=" + tail, null );
	}

	public String deleteContainer( String containerId, boolean force, boolean removeVolumes ) {
		return dockerService.getResponse( Request.Method.DELETE, "/containers/"+containerId+"?force="+force+"&v="+removeVolumes, null );
	}
	
	public String killContainer( String containerId ) {
		return dockerService.getResponse( Request.Method.POST, "/containers/"+containerId+"/kill", null );
	}

	public String reStartContainer( String containerId ) {
		logger.info("Reiniciando container " + containerId );
		return dockerService.getResponse( Request.Method.POST, "/containers/"+containerId+"/restart", null );
	}

	public String stopContainer( String containerId ) {
		logger.info("Stopping component " + containerId );
		return dockerService.getResponse( Request.Method.POST, "/containers/"+containerId+"/stop", null );
	}
	
	public String getContainerStats( String containerId ) {
		return dockerService.getResponse( Request.Method.GET, "/containers/"+containerId+"/stats?stream=false", null );
	}

	public String getContainers() {
		String result = dockerService.getResponse( Request.Method.GET, "/containers/json?all=true&size=true", null );
		return result;
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

}

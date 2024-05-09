package br.com.j1scorpii.ffmda.services;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class PostgreSQLService {
	private Logger logger = LoggerFactory.getLogger( PostgreSQLService.class );

	@Autowired private ImageManager imageManager;
	@Autowired private ContainerManager containerManager;
	@Autowired private LocalService localService;
	
	private final String COMPONENT_NAME = "postgresql";
	
	@Value("${ffmda.local.data.folder}")
	private String localDataFolder;	
	
	private String componentDataFolder;
	private String imageName;
	
	@PostConstruct
	private void init() {
		this.componentDataFolder = localDataFolder + "/" + COMPONENT_NAME;
		logger.info("init " + this.componentDataFolder );
		new File( this.componentDataFolder ).mkdirs();
	}

	public String startContainer() {
		
		JSONObject container = getContainer();
		if( container.has("State") ) {
			String state = container.getString("State");
			if( state.equals("running") ) return new JSONObject().put("result", "Already Running").toString();
			return new JSONObject().put("result", containerManager.startContainer( COMPONENT_NAME ) ).toString();
		}
		
		// We don't have any image yet. Pull it now
		if( this.imageName == null ) {
			this.pullImage();
			this.getConfig();
		}
		 
		JSONObject portBidings = new JSONObject();
		portBidings.put("5104", "5432/tcp");
		
		JSONArray envs = new JSONArray();
		envs.put("POSTGRES_PASSWORD=f1refly");
		envs.put("PGDATA=/var/lib/postgresql/data/pgdata");
		
		JSONArray volumes = new JSONArray();
		volumes.put("/etc/localtime:/etc/localtime:ro");
		volumes.put(  this.componentDataFolder + ":/var/lib/postgresql/data");
		
		JSONObject containerDef = new JSONObject();
		containerDef.put("name", COMPONENT_NAME);
		containerDef.put("ports", portBidings );
		containerDef.put("image", this.imageName );
		containerDef.put("connectToNetwork", "ffmda");
		containerDef.put("restart", "always");
		containerDef.put("environments", envs);
		containerDef.put("volumes", volumes);
		
		String result = this.containerManager.create( containerDef );
		containerDef.put("result", new JSONObject( result ) );
		
		return containerDef.toString();
	}
	
	public JSONObject imagePulled() {
		JSONObject result = new JSONObject();
		boolean exists = imageManager.exists(COMPONENT_NAME);
		result.put("exists", exists);
		if( exists ) {
			this.imageName = imageManager.getImageForComponent(COMPONENT_NAME);
			result.put("imageName", this.imageName );
		}
		return result;
	}
	
	private JSONObject getContainer() {
		JSONObject container = containerManager.getContainer( COMPONENT_NAME ); 
		return container;
	}

	public String pullImage() {
		return imageManager.pullImage(COMPONENT_NAME, true );
	}
	
	public String getConfig( ) {
		JSONObject localAgentConfig = localService.getMainConfig();
		// Use a object wrapper to send component configuration 
		// plus some relevant configuration to the UI.
		JSONObject generalConfig = new JSONObject();
		generalConfig.put("image", imagePulled() );
		generalConfig.put("container", getContainer() );
		// Plus the local node config ( I need this server's IP and host )
		generalConfig.put("localAgentConfig", localAgentConfig );
		return generalConfig.toString(5);
	}
	

	public String restartContainer() {
		return new JSONObject().put("result", this.containerManager.reStartContainer( COMPONENT_NAME) ).toString();
	}

}

package br.com.j1scorpii.ffmda.services;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient.Request;
import com.github.dockerjava.transport.DockerHttpClient.Request.Builder;
import com.github.dockerjava.transport.DockerHttpClient.Request.Method;
import com.github.dockerjava.transport.DockerHttpClient.Response;

import jakarta.annotation.PostConstruct;

@Service
public class DockerService {
	private Logger logger = LoggerFactory.getLogger(DockerService.class);
	private DockerHttpClient httpClient;
	private DockerClientConfig config;
	private DockerClient dockerClient;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	
	@Value("${spring.profiles.active}")
	private String activeProfile;

	@PostConstruct
	private void init() {
		logger.info("init");
		// If active profile is DEV then I can't activate Docker socket because I'm on
		// Windows.
		if (!this.activeProfile.equals("dev")) {
			this.config = DefaultDockerClientConfig.createDefaultConfigBuilder()
					.withDockerHost("unix:///var/run/docker.sock").build();

			this.httpClient = new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost())
					.maxConnections(100).connectionTimeout(Duration.ofSeconds(30))
					.responseTimeout(Duration.ofSeconds(45)).build();

			this.dockerClient = DockerClientImpl.getInstance(this.config, this.httpClient);

			logger.info("Docker Service Status: " + pingDockerService());
		}
	}

	public void pullImage(String imageName, String callBackChannel	) {
		
		PullImageResultCallback callback = new PullImageResultCallback() {
			
			@Override
			public void onNext(PullResponseItem item) {
				messagingTemplate.convertAndSend( callBackChannel, new JSONObject( item ).put("imageName", imageName).toString() );
				super.onNext(item);
			}
			
			@Override
			public void onComplete() {
				messagingTemplate.convertAndSend( callBackChannel, new JSONObject( ).put("status", "Pull Complete.").put("imageName", imageName).toString() );
				super.onComplete();
			}
			
			@Override
			public void onStart(Closeable stream) {
				messagingTemplate.convertAndSend( callBackChannel, new JSONObject( ).put("status", "Pull Start").put("imageName", imageName).toString() );
				super.onStart(stream);
			}
			
			@Override
			public void onError(Throwable throwable) {
				messagingTemplate.convertAndSend( callBackChannel, new JSONObject( ).put("status", "Error: " + throwable.getMessage() ).put("imageName", imageName).toString() );
				super.onError(throwable);
			}
			
		};

		try {
			this.dockerClient.pullImageCmd( imageName ).exec( callback ).awaitCompletion();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	private String pingDockerService() {
		return getResponse(Request.Method.GET, "/_ping", null);
	}

	public String getResponse(Method method, String path, JSONObject body) {

		Builder builder = Request.builder().method(method).path(path);

		if (method == Method.POST)
			builder = builder.headers(headers());
		if (body != null)
			builder = builder.body(createIs(body));

		Request request = builder.build();

		try {
			Response response = httpClient.execute(request);
			return IOUtils.toString(response.getBody(), "UTF-8");
		} catch (Exception e) {
			logger.error(e.getMessage());
			return e.getMessage();
		}
	}

	private InputStream createIs(JSONObject body) {
		return new ByteArrayInputStream(body.toString().getBytes());
	}

	private Map<String, String> headers() {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		return headers;
	}

}

package net.rezxis.mchosting.host;

import java.net.URI;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;

import net.rezxis.mchosting.database.Database;
import net.rezxis.mchosting.host.managers.games.CustomDockerManager;
import net.rezxis.mchosting.host.managers.games.DockerManager;
import net.rezxis.mchosting.network.WSClient;

public class HostServer {

	public static Props props;
	public static WSClient client;
	public static DockerManager dManager;
	public static CustomDockerManager cManager;
	public static DockerClient dClient;
	public static int currentPort = 27000;
	
	public static void main(String[] args) {
		props = new Props("host.propertis");
		Database.init(props.DB_HOST,props.DB_USER,props.DB_PASS,props.DB_PORT,props.DB_NAME);
		// init server managers
		dClient = connect();
		dManager = new DockerManager(dClient);
		cManager = new CustomDockerManager(dClient);
		WorkerThread.dMgr = dManager;
		WorkerThread.cMgr = cManager;
		try {
			client = new WSClient(new URI("ws://"+props.SYNC_ADDRESS+":"+props.SYNC_PORT),  new WSClientHandler());
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("failed to init websocket.");
			return;
		}
		new Thread(new HealthCheckTask()).start();
		client.connect();
	}
	
	public static DockerClient connect() {
		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
				  .build();
		@SuppressWarnings("resource")
		DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
				  .withConnectTimeout(1000)
				  .withMaxTotalConnections(100)
				  .withMaxPerRouteConnections(10);
		return DockerClientBuilder.getInstance(config)
				  .withDockerCmdExecFactory(dockerCmdExecFactory)
				  .build();
	}
}

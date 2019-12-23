package net.rezxis.mchosting.host;

import java.net.URI;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;

import net.rezxis.mchosting.database.Database;
import net.rezxis.mchosting.database.tables.BackupsTable;
import net.rezxis.mchosting.database.tables.PlayersTable;
import net.rezxis.mchosting.database.tables.PluginsTable;
import net.rezxis.mchosting.database.tables.ServersTable;
import net.rezxis.mchosting.host.managers.games.CustomDockerManager;
import net.rezxis.mchosting.host.managers.games.DockerManager;
import net.rezxis.mchosting.network.WSClient;

public class HostServer {

	public static Props props;
	public static WSClient client;
	public static ServersTable sTable;
	public static PluginsTable plTable;
	public static PlayersTable psTable;
	public static BackupsTable bTable;
	public static DockerManager dManager;
	public static CustomDockerManager cManager;
	public static DockerClient dClient;
	public static int currentPort = 27000;
	
	public static void main(String[] args) {
		props = new Props("host.propertis");
		Database.init();
		sTable = new ServersTable();
		plTable = new PluginsTable();
		psTable = new PlayersTable();
		bTable = new BackupsTable();
		
		// init server managers
		dClient = connect(props.DOCKER_ADDRESS, props.DOCKER_PORT);
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
		client.connect();
	}
	
	public static DockerClient connect(String host, int port) {
		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
				  .withDockerHost("tcp://"+host+":"+port)
				  .build();
		DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
				  .withConnectTimeout(1000)
				  .withMaxTotalConnections(100)
				  .withMaxPerRouteConnections(10);
		return DockerClientBuilder.getInstance(config)
				  .withDockerCmdExecFactory(dockerCmdExecFactory)
				  .build();
	}
}

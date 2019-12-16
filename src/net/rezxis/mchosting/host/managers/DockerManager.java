package net.rezxis.mchosting.host.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import com.google.gson.Gson;

import net.rezxis.mchosting.database.DBPlayer;
import net.rezxis.mchosting.database.DBServer;
import net.rezxis.mchosting.database.ServerStatus;
import net.rezxis.mchosting.host.HostServer;
import net.rezxis.mchosting.host.game.GameMaker;
import net.rezxis.mchosting.host.game.MCProperties;
import net.rezxis.mchosting.network.packet.host.HostRebootServer;
import net.rezxis.mchosting.network.packet.host.HostStartServer;
import net.rezxis.mchosting.network.packet.host.HostStoppedServer;

public class DockerManager {

	private static Gson gson = new Gson();
	private static DockerClient client;
	private static String prefix = "container";
	private static String imgName = "rezxis/rzmc:latest";
	private static int currentPort = 27000;
	//<serverID:ContainerID>
	private static HashMap<Integer,String> ids = new HashMap<>();
	
	public static void connect(String host, int port) {
		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
				  .withDockerHost("tcp://"+host+":"+port)
				  
				  .build();
		DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
				  .withConnectTimeout(1000)
				  .withMaxTotalConnections(100)
				  .withMaxPerRouteConnections(10);
		client = DockerClientBuilder.getInstance(config)
				  .withDockerCmdExecFactory(dockerCmdExecFactory)
				  .build();
	}
	
	public static void reboot(String json) {
		HostRebootServer packet = gson.fromJson(json, HostRebootServer.class);
		DBServer server = HostServer.sTable.getByID(packet.id);
		try {
			Thread.sleep(5000);
			System.out.println("waited for 5s to start container for reboot server");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		client.startContainerCmd(ids.get(server.getID())).exec();
	}
	
	public static int runningServers() {
		return client.infoCmd().exec().getContainersRunning();
	}
	
	public static void stopped(String json) {
		HostStoppedServer p = gson.fromJson(json, HostStoppedServer.class);
		DBServer server = HostServer.sTable.get(UUID.fromString(p.player));
		client.removeContainerCmd(ids.get(server.getID())).withForce(true).exec();
		ids.remove(server.getID());
	}
	
	public static void kill(String json) {
		HostStartServer packet = gson.fromJson(json, HostStartServer.class);
		DBServer server = HostServer.sTable.get(UUID.fromString(packet.player));
		if (server == null) {
			System.out.println("The server was not found");
			return;
		}
		client.killContainerCmd(ids.get(server.getID())).exec();
		server.setStatus(ServerStatus.STOP);
		server.setPlayers(0);
		server.setPort(-1);
		server.update();
	}
	
	public static void start(String json) {
		HostStartServer packet = gson.fromJson(json, HostStartServer.class);
		DBServer server = HostServer.sTable.get(UUID.fromString(packet.player));
		if (server == null) {
			System.out.println("The server was not found");
			return;
		}
		if (runningServers() > HostServer.props.MAX_SERVERS) {
			System.out.println("There are no space to start server");
			return;
		}
		currentPort += 1;
		final int port = currentPort;
		server.setStatus(ServerStatus.STARTING);
		server.setPort(port);
		server.update();
		MCProperties props = new MCProperties(String.valueOf(server.getID()), port, server.getWorld(), server.getCmd());
		try {
			props.generateFile(new File("servers/"+server.getID()));
			PluginManager.checkPlugins(server);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("couldn't initialize plugins.");
			server.setStatus(ServerStatus.STOP);
			server.update();
			return;
		}
		DBPlayer player = HostServer.psTable.get(server.getOwner());
		GameMaker gm = new GameMaker(new File(HostServer.props.SERVER_JAR_NAME),new File("servers/"+server.getID()), player.getRank().getMem());
		gm.setMaxPlayer(player.getRank().getMaxPlayers());
		gm.setPort(port);
		Volume volSpigot = new Volume("/spigot");
		Volume volServer = new Volume("/data");
		Bind bindSpigot = new Bind(new File("spigot/").getAbsolutePath(),volSpigot);
		Bind bindServer = new Bind(new File("servers/"+server.getID()).getAbsolutePath(),volServer);
		ExposedPort eport = ExposedPort.tcp(port);
		Ports portBindings = new Ports();
		portBindings.bind(eport, Ports.Binding.bindPort(port));
		ArrayList<String> list = new ArrayList<>();
		list.add("MAX_MEMORY="+player.getRank().getMem());
		//long mem = Integer.valueOf(player.getRank().getMem().replace("G", "")) * 1024;
		CreateContainerResponse container = client.createContainerCmd(imgName)
				.withVolumes(volSpigot,volServer)
				.withName(prefix+server.getID())
				.withTty(true)
				.withBinds(bindSpigot,bindServer)
				.withExposedPorts(eport)
				.withPortBindings(portBindings)
				.withEnv(list)
				//.withMemory(mem)
				.exec();
		ids.put(server.getID(), container.getId());
		client.startContainerCmd(container.getId()).exec();
	}
}

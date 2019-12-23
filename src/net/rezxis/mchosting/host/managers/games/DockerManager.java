package net.rezxis.mchosting.host.managers.games;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

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

import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.object.server.ServerStatus;
import net.rezxis.mchosting.host.HostServer;
import net.rezxis.mchosting.host.game.GameMaker;
import net.rezxis.mchosting.host.game.MCProperties;
import net.rezxis.mchosting.host.managers.IGame;
import net.rezxis.mchosting.host.managers.PluginManager;
import net.rezxis.mchosting.network.packet.host.HostRebootServer;
import net.rezxis.mchosting.network.packet.host.HostStartServer;
import net.rezxis.mchosting.network.packet.host.HostStoppedServer;

public class DockerManager implements IGame {

	private static DockerManager instance = null;
	
	public DockerManager(DockerClient client) {
		instance = this;
		this.client = client;
	}
	
	public static DockerManager getInstance() {
		return instance;
	}
	
	private final static String prefix = "container";
	private final static String imgName = "rezxis/rzmc:latest";
	
	private DockerClient client;
	private HashMap<Integer,String> ids = new HashMap<>();
	
	public int runningServers() {
		return client.infoCmd().exec().getContainersRunning();
	}
	
	public void reboot(DBServer target) {
		try {
			Thread.sleep(5000);
			System.out.println("waited for 5s to start container for reboot target");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			PluginManager.checkPlugins(target);
		} catch (Exception e) {
			e.printStackTrace();
			target.setStatus(ServerStatus.STOP);
			target.update();
		}
		client.startContainerCmd(ids.get(target.getId())).exec();
	}
	
	public void stopped(DBServer target) {
		client.removeContainerCmd(ids.get(target.getId())).withForce(true).exec();
		ids.remove(target.getId());
	}
	
	public void kill(DBServer target) {
		if (target == null) {
			System.out.println("The target was not found");
			return;
		}
		client.killContainerCmd(ids.get(target.getId())).exec();
		client.removeContainerCmd(ids.get(target.getId())).exec();
		target.setStatus(ServerStatus.STOP);
		target.setPlayers(0);
		target.setPort(-1);
		target.update();
	}
	
	public void start(DBServer target) {
		if (target == null) {
			System.out.println("The target was not found");
			return;
		}
		if (runningServers() > HostServer.props.MAX_SERVERS) {
			System.out.println("There are no space to start target");
			return;
		}
		DBPlayer player = HostServer.psTable.get(target.getOwner());
		HostServer.currentPort += 1;
		final int port = HostServer.currentPort;
		target.setStatus(ServerStatus.STARTING);
		target.setPort(port);
		target.update();
		MCProperties props = new MCProperties(target,player);
		try {
			props.generateFile(new File("servers/"+target.getId()));
			PluginManager.checkPlugins(target);
			if (new File(new File("servers/"+target.getId()),"logs").exists())
				FileUtils.forceDelete(new File(new File("servers/"+target.getId()),"logs"));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("couldn't initialize plugins.");
			target.setStatus(ServerStatus.STOP);
			target.update();
			return;
		}
		GameMaker gm = new GameMaker(new File(HostServer.props.SERVER_JAR_NAME),new File("servers/"+target.getId()), player.getRank().getMem());
		gm.setMaxPlayer(player.getRank().getMaxPlayers());
		gm.setPort(port);
		Volume volSpigot = new Volume("/spigot");
		Volume volServer = new Volume("/data");
		Bind bindSpigot = new Bind(new File("spigot/").getAbsolutePath(),volSpigot);
		Bind bindServer = new Bind(new File("servers/"+target.getId()).getAbsolutePath(),volServer);
		ExposedPort eport = ExposedPort.tcp(port);
		Ports portBindings = new Ports();
		portBindings.bind(eport, Ports.Binding.bindPort(port));
		ArrayList<String> list = new ArrayList<>();
		list.add("MAX_MEMORY="+player.getRank().getMem());
		//long mem = Integer.valueOf(player.getRank().getMem().replace("G", "")) * 1024;
		CreateContainerResponse container = client.createContainerCmd(imgName)
				.withVolumes(volSpigot,volServer)
				.withName(prefix+target.getId())
				.withTty(true)
				.withBinds(bindSpigot,bindServer)
				.withExposedPorts(eport)
				.withPortBindings(portBindings)
				.withEnv(list)
				//.withMemory(mem)
				.exec();
		ids.put(target.getId(), container.getId());
		client.startContainerCmd(container.getId()).exec();
	}
}

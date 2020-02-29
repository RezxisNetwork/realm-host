package net.rezxis.mchosting.host.managers.games;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.LogConfig;
import com.github.dockerjava.api.model.LogConfig.LoggingType;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;

import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.player.DBPlayer.Rank;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.object.server.ServerStatus;
import net.rezxis.mchosting.host.HostServer;
import net.rezxis.mchosting.host.game.GameMaker;
import net.rezxis.mchosting.host.game.MCProperties;
import net.rezxis.mchosting.host.managers.IGame;
import net.rezxis.mchosting.host.managers.PluginManager;
import net.rezxis.utils.WebAPI;
import net.rezxis.utils.WebAPI.DiscordWebHookEnum;

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
	
	public DockerClient client;
	
	public int runningServers() {
		return client.infoCmd().exec().getContainersRunning();
	}
	
	public void reboot(DBServer target) {
		String id = getConById(target.getId());
		boolean running = true;
		int time = 0;
		while (running) {
			if (time >= 10) {
				WebAPI.webhook(DiscordWebHookEnum.PRIVATE, String.format("[WARNING] A container restarting will not stop. ServerID : %s , Owner : %s , ContainerID : %s", String.valueOf(target.getId()),
						target.getOwner().toString(),id));
			}
			running = client.inspectContainerCmd(id).exec().getState().getRunning();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			time += 1;
		}
		DBPlayer player = Tables.getPTable().get(target.getOwner());
		try {
			MCProperties props = new MCProperties(target,player);
			props.generateFile(new File("servers/"+target.getId()));
			PluginManager.checkPlugins(target);
		} catch (Exception e) {
			e.printStackTrace();
			WebAPI.webhook(DiscordWebHookEnum.PRIVATE, e.getMessage());
			target.setStatus(ServerStatus.STOP);
			target.update();
			return;
		}
		client.startContainerCmd(id).exec();
	}
	
	public void stopped(DBServer target) {
		client.removeContainerCmd(getConById(target.getId())).withForce(true).exec();
		if (new File(new File("servers/"+target.getId()),"logs").exists())
			try {
				FileUtils.forceDelete(new File(new File("servers/"+target.getId()),"logs"));
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	public void kill(DBServer target) {
		String id = getConById(target.getId());
		if (id != null)
			client.removeContainerCmd(id).withForce(true).exec();
		target.setStatus(ServerStatus.STOP);
		target.setPlayers(0);
		target.setPort(-1);
		target.update();
		WebAPI.webhook(DiscordWebHookEnum.PRIVATE, String.format("killed a server ID : %s , UUID : %s", target.getId(), target.getOwner().toString()));
	}
	
	@SuppressWarnings("deprecation")
	public void start(DBServer target) {
		DBPlayer player = Tables.getPTable().get(target.getOwner());
		if (player.getRank() != Rank.OWNER || player.getRank() != Rank.SPECIAL || player.getRank() != Rank.DEVELOPER)
			if (runningServers() > HostServer.props.MAX_SERVERS) {
				System.out.println("There are no space to start target");
				return;
			}
		final int port = HostServer.currentPort;
		HostServer.currentPort += 1;
		target.setStatus(ServerStatus.STARTING);
		target.setPort(port);
		target.update();
		MCProperties props = new MCProperties(target,player);
		try {
			props.generateFile(new File("servers/"+target.getId()));
			PluginManager.checkPlugins(target);
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
		list.add("sync_address=ws://"+HostServer.props.DOCKER_GATEWAY+":"+HostServer.props.SYNC_PORT);
		list.add("db_host="+HostServer.props.DOCKER_GATEWAY);
		list.add("db_user="+HostServer.props.DB_USER);
		list.add("db_pass="+HostServer.props.DB_PASS);
		list.add("db_port="+HostServer.props.DB_PORT);
		list.add("db_name="+HostServer.props.DB_NAME);
		list.add("TZ=Asia/Tokyo");
		list.add("sowner="+target.getOwner().toString());
		//long mem = Integer.valueOf(player.getRank().getMem().replace("G", "")) * 1024;
		int cpu = 1024;
		switch (player.getRank()) {
		case NORMAL:
			cpu = 1024;
		case GOLD:
			cpu = 1024*(3/2);
		case DIAMOND:
			cpu = 1024*2;
		case EMERALD:
			cpu = 1024*(5/2);
		case SPECIAL:
			cpu = 1024*3;
		case DEVELOPER:
			cpu = 0;
		case STAFF:
			cpu = 0;
		case OWNER:
			cpu = 0;
		}
		CreateContainerResponse container = client.createContainerCmd(imgName)
				.withVolumes(volSpigot,volServer)
				.withName(prefix+target.getId())
				.withBinds(bindSpigot,bindServer)
				.withExposedPorts(eport)
				.withPortBindings(portBindings)
				.withEnv(list)
				.withCpuShares(cpu)
				.exec();
		client.startContainerCmd(container.getId()).exec();
	}
	
	private String getConById(int id) {
		return this.getContainerIDByName(prefix+id);
	}
	
	private String getContainerIDByName(String name) {
		ArrayList<String> list = new ArrayList<>();
		list.add(name);
		List<Container> containers = client.listContainersCmd().withNameFilter(list).exec();
		if (containers.size() == 0)
			return null;
		return containers.get(0).getId();
	}
}

package net.rezxis.mchosting.host.managers.games;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.github.dockerjava.api.model.*;
import org.apache.commons.io.FileUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.LogConfig.LoggingType;
import com.google.gson.Gson;

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
import net.rezxis.mchosting.network.packet.sync.SyncPlayerMessagePacket;
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
	private static Gson gson = new Gson();
	
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
		if (player.getRank() != Rank.OWNER || player.getRank() != Rank.SPECIAL || player.getRank() != Rank.DEVELOPER || !player.isSupporter())
			if (runningServers() > HostServer.props.MAX_SERVERS) {
				System.out.println("There are no space to start target");
				HostServer.client.send(gson.toJson(new SyncPlayerMessagePacket(target.getOwner(),"&aサーバーの起動上限に到達しているので、起動できません。")));
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
			HostServer.client.send(gson.toJson(new SyncPlayerMessagePacket(target.getOwner(),"&a内部エラーが発生したので、起動できません。Ticketで連絡してください。")));
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
		long cpu = 0;
		switch (player.getRank()) {
			case NORMAL:
				cpu = 100000;
			case GOLD:
				cpu = 150000;
			case DIAMOND:
				cpu = 200000;
			case EMERALD:
				cpu = 250000;
			case SPECIAL:
				cpu = 300000;
			default:
				cpu = 100000;
		}
		HostConfig hostConfig = new HostConfig();
		hostConfig
				.withBinds(bindSpigot,bindServer)
				.withPortBindings(portBindings)
				.withCpuPeriod(Long.valueOf(100000))
				.withCpuQuota(cpu);
		CreateContainerResponse container = null;
		try {
			container = client.createContainerCmd(imgName)
				.withVolumes(volSpigot,volServer)
				.withName(prefix+target.getId())
				.withExposedPorts(eport)
				.withEnv(list)
				.withHostConfig(hostConfig)
				.exec();
		} catch (Exception ex) {
			ex.printStackTrace();
			HostServer.client.send(gson.toJson(new SyncPlayerMessagePacket(target.getOwner(),"&a内部エラーが発生したため、起動できません。再度試しても起動できない場合、Ticketで連絡してください。")));
			target.setStatus(ServerStatus.STOP);
			target.update();
			return;
		}
		try {
			client.startContainerCmd(container.getId()).exec();
		} catch (Exception ex) {
			ex.printStackTrace();
			HostServer.client.send(gson.toJson(new SyncPlayerMessagePacket(target.getOwner(),"&a内部エラーが発生したため、起動できません。再度試しても起動できない場合、Ticketで連絡してください。")));
			target.setStatus(ServerStatus.STOP);
			target.update();
		}
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

package net.rezxis.mchosting.host.managers.games;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.dockerjava.api.model.*;
import org.apache.commons.io.FileUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
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
		int count = 0;
		for (Container c : client.listContainersCmd().exec()) {
			for (String s : c.getNames()) {
				if (s.startsWith("/container")) {
					++count;
				}
			}
		}
		return count;
	}
	
	@SuppressWarnings("deprecation")
	public void reboot(DBServer target) {
		String id = getConById(target.getId());
		boolean running = true;
		int time = 0;
		while (running) {
			if (time >= 10) {
				WebAPI.webhook(DiscordWebHookEnum.PRIVATE, String.format("[WARNING] A container restarting will not stop. ServerID : %s , Owner : %s , ContainerID : %s", String.valueOf(target.getId()),
						target.getOwner().toString(),id));
				client.stopContainerCmd(id).exec();
				time = 0;
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
		target.setIp(client.inspectContainerCmd(id).exec().getNetworkSettings().getIpAddress());
		target.update();
	}
	
	public void stopped(DBServer target) {
		client.removeContainerCmd(getConById(target.getId())).withForce(true).exec();
		if (new File(new File("servers/"+target.getId()),"logs").exists())
			try {
				FileUtils.forceDelete(new File(new File("servers/"+target.getId()),"logs"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		target.setStatus(ServerStatus.STOP);
		target.update();
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
		if (!(player.getRank() == Rank.OWNER || player.getRank() == Rank.SPECIAL || player.getRank() == Rank.DEVELOPER) )
			if (runningServers() > HostServer.props.MAX_SERVERS) {
				if (!player.isSupporter()) {
					System.out.println("There are no space to start target");
					HostServer.client.send(gson.toJson(new SyncPlayerMessagePacket(target.getOwner(),"&aサーバーの起動上限に到達しているので、起動できません。")));
					return;
				}
			}
		final int port = 25565;//HostServer.currentPort;#port-test
		//HostServer.currentPort += 1;#port-test
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
		//ExposedPort eport = ExposedPort.tcp(port);#port-test
		//Ports portBindings = new Ports();#port-test
		//portBindings.bind(eport, Ports.Binding.bindPort(port));#port-test
		ArrayList<String> list = new ArrayList<>();
		list.add("MAX_MEMORY="+player.getRank().getMem());
		list.add("sync_address=ws://"+HostServer.props.CHILD_SYNC+":"+HostServer.props.SYNC_PORT);
		list.add("db_host="+HostServer.props.CHILD_DB);
		list.add("db_user="+HostServer.props.DB_USER);
		list.add("db_pass="+HostServer.props.DB_PASS);
		list.add("db_port="+HostServer.props.DB_PORT);
		list.add("db_name="+HostServer.props.DB_NAME);
		list.add("TZ=Asia/Tokyo");
		list.add("sowner="+target.getOwner().toString());
		list.add("MC_VERSION="+target.getVersion().name());
		//long mem = Integer.valueOf(player.getRank().getMem().replace("G", "")) * 1024;
		long cpu = 0;
		switch (player.getRank()) {
			case NORMAL:
				cpu = 200000;//100000;
			case GOLD:
				cpu = 250000;//150000;
			case DIAMOND:
				cpu = 300000;//200000;
			case EMERALD:
				cpu = 350000;//250000;
			case SPECIAL:
				cpu = 350000;//300000;
			default:
				cpu = 200000;//100000;
		}
		HostConfig hostConfig = new HostConfig();
		hostConfig
				.withBinds(bindSpigot,bindServer);//#port-test
				//.withPortBindings(portBindings);#port-test
		if(HostServer.props.CPULIMIT){
			hostConfig
					.withCpuPeriod(Long.valueOf(100000))
					.withCpuQuota(cpu);
		}
		CreateContainerResponse container = null;
		try {
			String tmp = this.getConById(target.getId());
			if (tmp != null) {
				client.removeContainerCmd(tmp).withForce(true).exec();
			}
			container = client.createContainerCmd(imgName)
				.withVolumes(volSpigot,volServer)
				.withName(prefix+target.getId())
				//.withExposedPorts(eport)#port-test
				.withEnv(list)
				.withHostConfig(hostConfig)
				.withAttachStderr(true)
				.withAttachStdin(true)
				.withStdInOnce(true)
				.withStdinOpen(true)
				.withTty(true)
				.withAttachStdout(true)
				.withOomKillDisable(true)
				.exec();
		} catch (Exception ex) {
			ex.printStackTrace();
			HostServer.client.send(gson.toJson(new SyncPlayerMessagePacket(target.getOwner(),"&a内部エラーが発生したため、起動できません。再度試しても起動できない場合、Ticketで連絡してください。")));
			target.setStatus(ServerStatus.STOP);
			target.update();
			String id = getConById(target.getId());
			if (id != null) {
				client.removeContainerCmd(id).withForce(true).exec();
			}
			return;
		}
		try {
			client.connectToNetworkCmd().withContainerId(container.getId()).withNetworkId("9e55219097c90c62d827ec369dc7b98d80d5057daf7243b9cd8204521f2e14a2").exec();
			client.startContainerCmd(container.getId()).exec();
			target.setIp(client.inspectContainerCmd(container.getId()).exec().getNetworkSettings().getIpAddress());
			target.update();
		} catch (Exception ex) {
			ex.printStackTrace();
			HostServer.client.send(gson.toJson(new SyncPlayerMessagePacket(target.getOwner(),"&a内部エラーが発生したため、起動できません。再度試しても起動できない場合、Ticketで連絡してください。")));
			target.setStatus(ServerStatus.STOP);
			target.update();
			String id = getConById(target.getId());
			if (id != null) {
				client.removeContainerCmd(id).withForce(true).exec();
			}
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

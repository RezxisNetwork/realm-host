package net.rezxis.mchosting.host.managers.games;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
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
		if (new File(new File("servers/"+target.getId()),"logs").exists())
			try {
				FileUtils.forceDelete(new File(new File("servers/"+target.getId()),"logs"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	public void kill(DBServer target) {
		if (target == null) {
			System.out.println("The target was not found");
			return;
		}
		if (!ids.containsKey(target.getId())) {
			target.setStatus(ServerStatus.STOP);
			target.setPlayers(0);
			target.setPort(-1);
			target.update();
			return;
		}
		client.removeContainerCmd(ids.get(target.getId())).withForce(true).exec();
	}
	
	@SuppressWarnings("deprecation")
	public void start(DBServer target) {
		if (target == null) {
			System.out.println("The target was not found");
			return;
		}
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
		HashMap<String,String> logConfMap = new HashMap<>();
		logConfMap.put("tag", target.getOwner().toString());
		LogConfig logConfig = new LogConfig().setType(LoggingType.JSON_FILE).setConfig(logConfMap);
		CreateContainerResponse container = client.createContainerCmd(imgName)
				.withVolumes(volSpigot,volServer)
				.withName(prefix+target.getId())
				.withTty(true)
				.withBinds(bindSpigot,bindServer)
				.withExposedPorts(eport)
				.withPortBindings(portBindings)
				.withEnv(list)
				.withLogConfig(logConfig)
				.exec();
		ids.put(target.getId(), container.getId());
		client.startContainerCmd(container.getId()).exec();
	}
}

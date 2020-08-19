package net.rezxis.mchosting.host.managers.games;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import com.google.gson.Gson;

import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.object.server.ServerStatus;
import net.rezxis.mchosting.host.HostServer;
import net.rezxis.mchosting.host.managers.IGame;
import net.rezxis.mchosting.host.managers.PluginManager;
import net.rezxis.mchosting.network.packet.sync.SyncCustomStarted;

public class CustomDockerManager implements IGame {

	private final static String prefix = "container";
	private final static String imgName = "rezxis/custom:latest";
	
	private static CustomDockerManager instance;
	private DockerClient client;
	
	public static CustomDockerManager getInstance() {
		return instance;
	}
	
	public CustomDockerManager(DockerClient client) {
		this.client = client;
		instance = this;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void start(DBServer target) {
		updateSync(target,ServerStatus.STARTING);
		File data = new File("servers/"+target.getId());
		try {
			if (!data.exists()) {
				data.mkdirs();
			}
			FileUtils.copyFile(new File("spigot/spigot.jar"), new File(data,"spigot.jar"));
			
			//mc props and spigot.yml to use bungeecord.
			this.initProps(new File(data,"server.properties"), target);
			FileUtils.copyFile(new File("base/spigot.yml"), new File(data,"spigot.yml"));
		} catch (Exception ex) {
			ex.printStackTrace();
			updateSync(target,ServerStatus.STOP);
			System.out.println("An exeception in init custom server file.");
			return;
		}
		DBPlayer player = Tables.getPTable().get(target.getOwner());
		//create container and start - skid from DockerManager
		Volume vol = new Volume("/data");
		Bind bind = new Bind(new File("servers/"+target.getId()).getAbsolutePath(),vol);
		int port = 25565;//HostServer.currentPort;#port-test
		//HostServer.currentPort += 1;#port-test
		ExposedPort eport = ExposedPort.tcp(25565);
		Ports bindings = new Ports();
		bindings.bind(eport, Ports.Binding.bindPort(port));
		ArrayList<String> list = new ArrayList<>();
		list.add("MAX_MEMORY="+player.getRank().getMem());
		CreateContainerResponse container = client.createContainerCmd(imgName)
				.withVolumes(vol)
				.withName(prefix+target.getId())
				.withTty(true)
				.withBinds(bind)
				.withExposedPorts(eport)
				.withPortBindings(bindings)
				.withEnv(list)
				//.withMemory(mem)
				.exec();
		
		client.startContainerCmd(container.getId()).exec();
		updateSync(target,ServerStatus.RUNNING);
		String ip = null;
		for (Network net : client.listNetworksCmd().exec()) {
			if (net.getName().equalsIgnoreCase("bridge")) {
				ip = net.getContainers().get(prefix+target.getId()).getIpv4Address();
			}
		}
		SyncCustomStarted packet = new SyncCustomStarted(target.getId(),ip);
		HostServer.client.send(new Gson().toJson(packet));
	}

	private void initProps(File dir,DBServer server) throws Exception {
		if (!dir.exists()) {
			if (!dir.getParentFile().exists())
				dir.getParentFile().mkdirs();
			dir.createNewFile();
		}
		BufferedReader br = new BufferedReader(new FileReader(dir));
		HashMap<String, String> values = new HashMap<>();
		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("=")) {
				if (line.length() == 2) {
					values.put(line.split("=")[0], line.split("=")[1]);
				} else {
					values.put(line.split("=")[0], "");
				}
			}
		}
		br.close();
		values.put("offline-mode", "false");
		values.put("enable-command-block", String.valueOf(server.isCmd()));
		PrintWriter pw = new PrintWriter(dir);
		for (Entry<String,String> set : values.entrySet()) {
			pw.println(set.getKey()+"="+set.getValue());
		}
		pw.close();
	}
	
	@Override
	public void stopped(DBServer target) {
		client.removeContainerCmd(getConById(target.getId())).withForce(true).exec();
	}

	@Override
	public void kill(DBServer target) {
		if (target == null) {
			System.out.println("The target was not found");
			return;
		}
		client.killContainerCmd(getConById(target.getId())).exec();
		client.removeContainerCmd(getConById(target.getId())).exec();
		target.setStatus(ServerStatus.STOP);
		target.setPlayers(0);
		target.setPort(-1);
		target.update();
	}

	@Override
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
		client.startContainerCmd(getConById(target.getId())).exec();
	}
	
	private void updateSync(DBServer s, ServerStatus t) {
		s.setStatus(t);
		s.update();
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

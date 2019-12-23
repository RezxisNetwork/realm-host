package net.rezxis.mchosting.host.managers.games;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;

import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.object.server.ServerStatus;
import net.rezxis.mchosting.host.HostServer;
import net.rezxis.mchosting.host.managers.IGame;
import net.rezxis.mchosting.host.managers.PluginManager;

public class CustomDockerManager implements IGame {

	private final static String prefix = "container";
	private final static String imgName = "rezxis/custom:latest";
	
	private static CustomDockerManager instance;
	private DockerClient client;
	private HashMap<Integer,String> ids = new HashMap<>();
	
	public static CustomDockerManager getInstance() {
		return instance;
	}
	
	public CustomDockerManager(DockerClient client) {
		this.client = client;
		instance = this;
	}
	
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
		DBPlayer player = HostServer.psTable.get(target.getOwner());
		//create container and start - skid from DockerManager
		Volume vol = new Volume("/data");
		Bind bind = new Bind(new File("servers/"+target.getId()).getAbsolutePath(),vol);
		int port = HostServer.currentPort;
		HostServer.currentPort += 1;
		ExposedPort eport = ExposedPort.tcp(port);
		Ports bindings = new Ports();
		bindings.bind(eport, Ports.Binding.bindPort(25565));
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
		
		ids.put(target.getId(), container.getId());
		client.startContainerCmd(container.getId()).exec();
		updateSync(target,ServerStatus.RUNNING);
	}

	private void initProps(File dir,DBServer server) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(dir));
		HashMap<String, String> values = new HashMap<>();
		String line;
		while ((line = br.readLine()) != null) {
			if (!line.startsWith("#")) {
				values.put(line.split("=")[0], line.split("=")[1]);
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
		client.removeContainerCmd(ids.get(target.getId())).withForce(true).exec();
		ids.remove(target.getId());
	}

	@Override
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
		client.startContainerCmd(ids.get(target.getId())).exec();
	}
	
	private void updateSync(DBServer s, ServerStatus t) {
		s.setStatus(t);
		s.update();
	}
}

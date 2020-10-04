package net.rezxis.mchosting.host.managers.anni;

import java.io.File;
import java.util.ArrayList;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Volume;

import net.rezxis.mchosting.host.HostServer;

public class AnniGameMaker {

	@SuppressWarnings("unused")
	private String serverName;
	private String bukkitSettings = "bukkit.yml";
	private String commandsSettings = "commands.yml";
	private String config = "server.properties";
	@SuppressWarnings("unused")
	private boolean eulaArgee = true;
	private String host = ""; //empty is default
	private int maxPlayers = 120;
	private String plugins = "plugins";
	private int port = 25565;
	@SuppressWarnings("unused")
	private String spigotSettings = "spigot.yml";
	private File spigotJar;
	private File defDir;
	private int sid;
	public String cid;

	public AnniGameMaker(String serverName, File spigotJar, File defDir, int id) {
		if (!defDir.exists()) {
			defDir.mkdirs();
		}
		this.spigotJar = spigotJar;
		this.defDir = defDir;
		this.serverName = serverName;
		this.sid = id;
	}

	@SuppressWarnings("deprecation")
	public String build() {
		ArrayList<String> args = new ArrayList<String>();
		{
			args.add("java");
			args.add("-jar");
			args.add("-Xmx3G");

			args.add(this.spigotJar.getAbsolutePath());
			args.add("--bukkit-settings");
			args.add(this.bukkitSettings);
			args.add("--commands-settings");
			args.add(this.commandsSettings);
			args.add("--config");
			args.add(this.config);


			if (!this.host.isEmpty()) {
				args.add("--host");
				args.add(this.host);
			}
			args.add("--max-players");
			args.add(this.maxPlayers+"");
			args.add("--plugins");
			args.add(this.plugins);
			args.add("--port");
			args.add(this.port+"");
		} //setup args
		try {
			for (Container con : HostServer.dClient.listContainersCmd().exec()) {
				for (String name : con.getNames()) {
					if (name.equalsIgnoreCase("rezxis_ANNI_"+port)) {
						HostServer.dClient.startContainerCmd(con.getId()).exec();
						return con.getId();
					}
				}
			}
			Volume volData = new Volume(this.defDir.getAbsolutePath());
			Bind bindData = new Bind(this.defDir.getAbsolutePath(),volData);
			/*ExposedPort eport = ExposedPort.tcp(port);
			Ports portBindings = new Ports();
			portBindings.bind(eport, Ports.Binding.bindPort(port));*/	
			String id = HostServer.dClient.createContainerCmd("openjdk:8-jre")
					.withCmd(args)
					.withWorkingDir(this.defDir.getAbsolutePath())
					.withBinds(bindData)
					/*.withPortBindings(portBindings)*/
					.withName("rezxis_ANNI_"+port)
					.withNetworkMode("anni")
					.withTty(true)
					.withAttachStderr(true)
					.withIpv4Address("172.21.0."+(1+this.sid))
					.withAttachStdin(true)
					.withAttachStdout(true)
					.withStdInOnce(true)
					.withStdinOpen(true)
					.exec().getId();
			cid = id;
			return id;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public void start() {
		HostServer.dClient.startContainerCmd(cid).exec();
	}

	public void setBukkitSettings(String c) {
		this.bukkitSettings = c;
	}

	public void setCommandsSettings(String c) {
		this.commandsSettings = c;
	}

	public void setConfig(String c) {
		this.config = c;
	}

	public void setHost(String c) {
		this.host = c;
	}

	public void setMaxPlayer(int max) {
		this.maxPlayers = max;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setSpigotSettings(String c) {
		this.spigotSettings = c;
	}
}

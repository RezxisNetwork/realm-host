package net.rezxis.mchosting.host.game;

import java.io.File;
import java.util.ArrayList;

import net.rezxis.mchosting.host.HostServer;

public class GameMaker {

	private String bukkitSettings = "bukkit.yml";
	private String commandsSettings = "commands.yml";
	private String config = "server.properties";
	private String host = ""; //empty is default
	private int maxPlayers = 20;
	private String plugins = "plugins";
	private int port = 25565;
	private File spigotJar;
	private File defDir;
	private String mem;

	public GameMaker(File spigotJar, File defDir, String mem) {
		if (!defDir.exists()) {
			defDir.mkdirs();
		}
		this.spigotJar = spigotJar;
		this.defDir = defDir;
		this.mem = mem;
	}

	public Process runProcess() {
		ProcessBuilder pb = new ProcessBuilder();
		ArrayList<String> args = new ArrayList<String>();
		args.add("cmd");
		args.add("/C");
		if(HostServer.props.USE_WINDOW) {
			args.add("start");

		}

		args.add("java");
		args.add("-Xmx"+mem);
		args.add("-jar");

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

		pb.command(args);
		pb.directory(this.defDir);

		try {
			return pb.start();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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
}

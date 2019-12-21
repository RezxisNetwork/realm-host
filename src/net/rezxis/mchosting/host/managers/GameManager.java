package net.rezxis.mchosting.host.managers;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import com.google.gson.Gson;

import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.object.server.ServerStatus;
import net.rezxis.mchosting.host.HostServer;
import net.rezxis.mchosting.host.game.GameMaker;
import net.rezxis.mchosting.host.game.MCProperties;
import net.rezxis.mchosting.network.packet.host.HostRebootServer;
import net.rezxis.mchosting.network.packet.host.HostStartServer;
import net.rezxis.mchosting.network.packet.host.HostStopServer;
import net.rezxis.mchosting.network.packet.sync.SyncStoppedServer;

public class GameManager {

	public static Gson gson = new Gson();
	private static int currentPort = 27000;
	private static int runningServers = 0;
	//serverID:Process for force stop
	public static HashMap<Integer,Process> processes = new HashMap<>();
	
	public static void rebootServer(String json) {
		HostRebootServer packet = gson.fromJson(json, HostRebootServer.class);
		DBServer server = HostServer.sTable.getByID(packet.id);
		runServerIn(server, server.getPort());
	}
	
	public static void stoppedServer() {
		runningServers -= 1;
	}
	
	public static void startServer(String json) {
		HostStartServer packet = gson.fromJson(json, HostStartServer.class);
		DBServer server = HostServer.sTable.get(UUID.fromString(packet.player));
		if (server == null) {
			System.out.println("The server was not found");
			return;
		}
		if (runningServers > HostServer.props.MAX_SERVERS) {
			System.out.println("There are no space to start server");
			return;
		}
		new Thread(()->{
			server.setStatus(ServerStatus.STARTING);
			server.update();
			runServerIn(server, currentPort);
		}).start();
		runningServers += 1;
		currentPort += 1;
	}
	
	private static void runServerIn(DBServer server, int port) {
		try {
			DBPlayer player = HostServer.psTable.get(server.getOwner());
			MCProperties props = new MCProperties(server,player);
			try {
				props.generateFile(new File("servers/"+server.getID()));
				PluginManager.checkPlugins(server);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("couldn't initialize plugins.");
				return;
			}
			GameMaker gm = new GameMaker(new File(HostServer.props.SERVER_JAR_NAME),new File("servers/"+server.getID()), player.getRank().getMem());
			gm.setMaxPlayer(player.getRank().getMaxPlayers());
			gm.setPort(port);
			server.setPort(port);
			server.setPlayers(0);
			server.update();
			File logs = new File(new File("servers/"+server.getID()),"logs");
			if (logs.exists())
				logs.delete();
			processes.put(server.getID(), gm.runProcess());
		} catch (Exception ex) {
			ex.printStackTrace();
			server.setStatus(ServerStatus.STOP);
			server.update();
			runningServers -= 1;
		}
	}
	
	public static void forceStop(String json) {
		HostStopServer packet = gson.fromJson(json, HostStopServer.class);
		DBServer server = HostServer.sTable.get(UUID.fromString(packet.player));
		if (server == null)
			return;
		if (!processes.containsKey(server.getID()))
			return;
		HostServer.client.send(gson.toJson(new SyncStoppedServer(server.getPort())));
		server.setPort(-1);
		server.setStatus(ServerStatus.STOP);
		server.update();
		runningServers -= 1;
		processes.get(server.getID()).destroyForcibly();
	}
}

package net.rezxis.mchosting.host.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.java_websocket.WebSocket;

import com.google.gson.Gson;

import net.rezxis.mchosting.databse.DBServer;
import net.rezxis.mchosting.databse.ServerStatus;
import net.rezxis.mchosting.host.HostServer;
import net.rezxis.mchosting.host.game.GameMaker;
import net.rezxis.mchosting.host.game.MCProperties;
import net.rezxis.mchosting.host.game.ServerFileUtil;
import net.rezxis.mchosting.network.packet.host.HostCreateServer;
import net.rezxis.mchosting.network.packet.host.HostDeleteServer;
import net.rezxis.mchosting.network.packet.host.HostRebootServer;
import net.rezxis.mchosting.network.packet.host.HostStartServer;
import net.rezxis.mchosting.network.packet.host.HostStopServer;
import net.rezxis.mchosting.network.packet.sync.SyncServerCreated;
import net.rezxis.mchosting.network.packet.sync.SyncStoppedServer;

public class GameManager {

	public static Gson gson = new Gson();
	private static int currentPort = 27000;
	private static int runningServers = 0;
	//serverID:Process for force stop
	public static HashMap<Integer,Process> processes = new HashMap<>();
	
	public static void createServer(String json) {
		HostCreateServer createPacket = gson.fromJson(json, HostCreateServer.class);
		if (HostServer.sTable.get(UUID.fromString(createPacket.player)) != null) {
			System.out.println("A server to tried to create has already depolyed");
			return;
		}
		DBServer server = new DBServer(-1, createPacket.displayName, UUID.fromString(createPacket.player), -1, new ArrayList<>(),-1,ServerStatus.STOP,createPacket.world, HostServer.props.HOST_ID,"");
		HostServer.sTable.insert(server);
		ArrayList<String> array = new ArrayList<>();
		array.add("ViaVersion");
		server.setPlugins(array);
		server.update();
		new Thread(()->{
			ServerFileUtil.generateServerFile(String.valueOf(server.getID()), 1, server.getWorld());
			SyncServerCreated packet = new SyncServerCreated(server.getOwner().toString());
			HostServer.client.send(gson.toJson(packet));
		}).start();
	}
	
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
		MCProperties props = new MCProperties(String.valueOf(server.getID()), currentPort, server.getWorld());
		try {
			props.generateFile(new File("servers/"+server.getID()));
			PluginManager.checkPlugins(server);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("couldn't initialize plugins.");
			return;
		}
		GameMaker gm = new GameMaker(String.valueOf(server.getID()),new File(HostServer.props.SERVER_JAR_NAME),new File("servers/"+server.getID()));
		gm.setPort(port);
		server.setPort(port);
		server.setPlayers(0);
		server.update();
		processes.put(server.getID(), gm.runProcess());
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
	
	public static void deleteServer(String json) {
		HostDeleteServer packet = gson.fromJson(json, HostDeleteServer.class);
		new Thread(()->{
			try {
				new File("servers/"+packet.id).delete();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}).start();
	}
}

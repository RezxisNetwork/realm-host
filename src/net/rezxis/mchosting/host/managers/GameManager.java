package net.rezxis.mchosting.host.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.zeroturnaround.zip.ZipUtil;

import com.google.gson.Gson;

import net.rezxis.mchosting.databse.DBBackup;
import net.rezxis.mchosting.databse.DBPlayer;
import net.rezxis.mchosting.databse.DBServer;
import net.rezxis.mchosting.databse.DBShop;
import net.rezxis.mchosting.databse.ServerStatus;
import net.rezxis.mchosting.host.HostServer;
import net.rezxis.mchosting.host.RezxisHTTPAPI;
import net.rezxis.mchosting.host.game.GameMaker;
import net.rezxis.mchosting.host.game.MCProperties;
import net.rezxis.mchosting.host.game.ServerFileUtil;
import net.rezxis.mchosting.network.packet.enums.BackupAction;
import net.rezxis.mchosting.network.packet.host.HostBackupPacket;
import net.rezxis.mchosting.network.packet.host.HostCreateServer;
import net.rezxis.mchosting.network.packet.host.HostDeleteServer;
import net.rezxis.mchosting.network.packet.host.HostRebootServer;
import net.rezxis.mchosting.network.packet.host.HostStartServer;
import net.rezxis.mchosting.network.packet.host.HostStopServer;
import net.rezxis.mchosting.network.packet.host.HostWorldPacket;
import net.rezxis.mchosting.network.packet.host.HostWorldPacket.Action;
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
		DBServer server = new DBServer(-1, createPacket.displayName,
				UUID.fromString(createPacket.player), -1, new ArrayList<>(),
				-1,ServerStatus.STOP,createPacket.world, HostServer.props.HOST_ID,
				"",true,true,"EMERALD_BLOCK", new DBShop(new ArrayList<>()),0);
		HostServer.sTable.insert(server);
		ArrayList<String> array = new ArrayList<>();
		array.add("ViaVersion");
		server.setPlugins(array);
		server.update();
		new Thread(()->{
			ServerFileUtil.generateServerFile(String.valueOf(server.getID()), 1, server.getWorld(), server.getCmd());
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
			MCProperties props = new MCProperties(String.valueOf(server.getID()), currentPort, server.getWorld(), server.getCmd());
			try {
				props.generateFile(new File("servers/"+server.getID()));
				PluginManager.checkPlugins(server);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("couldn't initialize plugins.");
				return;
			}
			DBPlayer player = HostServer.psTable.get(server.getOwner());
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
	
	public static void world(String json) {
		HostWorldPacket packet = gson.fromJson(json, HostWorldPacket.class);
		if (packet.action == Action.UPLOAD)
			uploadWorld(packet);
		else
			downloadWorld(packet);
	}
	
	private static void uploadWorld(HostWorldPacket packet) {
		long time = System.currentTimeMillis();
		String uuid = packet.values.get("uuid");
		String secret = packet.values.get("secret");
		DBServer server = HostServer.sTable.get(UUID.fromString(uuid));
		File cacheDir = new File("cache");
		if (!cacheDir.exists())
			cacheDir.mkdirs();
		File cache = new File(cacheDir, uuid+"world.zip");
		try {
			RezxisHTTPAPI.download(cache, secret, uuid);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		if (!isZipFile(cache)) {
			return;
		}
		File dest = new File("servers/"+server.getID()+"/world");
		dest.delete();
		dest.mkdir();
		ZipUtil.unpack(cache, dest);
		cache.delete();
		System.out.println("world upload takes "+(System.currentTimeMillis()-time)+"ms");
	}
	
	private static boolean isZipFile(File f) {
        if (f.isDirectory())
            return false;
        try {
            ZipFile file = new ZipFile(f);
            return true;
        } catch (ZipException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
	
	public static void backup(String json) {
		HostBackupPacket packet = gson.fromJson(json, HostBackupPacket.class);
		DBServer server = HostServer.sTable.get(UUID.fromString(packet.owner));
		if (server == null) {
			System.out.print("received a backup request from no server.");
			return;
		}
		if (packet.action == BackupAction.TAKE) {
			if (!new File("backups").exists()) {
				new File("backups").mkdirs();
			}
			DBBackup obj = new DBBackup(-1, HostServer.props.HOST_ID, packet.owner, packet.value.get("name"), new Date());
			server.setStatus(ServerStatus.BACKUP);
			server.update();
			HostServer.bTable.insert(obj);
			try {
				ZipUtil.pack(new File("servers/"+server.getID()), new File("backups/"+obj.getId()+".zip"));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			server.setStatus(ServerStatus.STOP);
			server.update();
		} else if (packet.action == BackupAction.DELETE) {
			DBBackup obj = HostServer.bTable.getBackupFromID(Integer.valueOf(packet.value.get("id")));
			File file = new File("backups/"+obj.getId()+".zip");
			if (file.exists()) {
				file.delete();
			}
			HostServer.bTable.delete(obj);
		} else if (packet.action == BackupAction.PATCH) {
			DBBackup obj = HostServer.bTable.getBackupFromID(Integer.valueOf(packet.value.get("id")));
			server.setStatus(ServerStatus.BACKUP);
			server.update();
			File sFile = new File("servers/"+server.getID());
			sFile.delete();
			ZipUtil.unpack(new File("backups/"+obj.getId()+".zip"), sFile);
			server.setStatus(ServerStatus.STOP);
			server.update();
		}
	}
	
	private static void downloadWorld(HostWorldPacket packet) {
		
	}
}

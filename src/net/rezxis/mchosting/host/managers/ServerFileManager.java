package net.rezxis.mchosting.host.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.zip.ZipUtil;

import com.google.gson.Gson;

import net.rezxis.mchosting.database.DBBackup;
import net.rezxis.mchosting.database.DBServer;
import net.rezxis.mchosting.database.DBShop;
import net.rezxis.mchosting.database.ServerStatus;
import net.rezxis.mchosting.host.HostServer;
import net.rezxis.mchosting.host.RezxisHTTPAPI;
import net.rezxis.mchosting.host.game.ServerFileUtil;
import net.rezxis.mchosting.network.packet.enums.BackupAction;
import net.rezxis.mchosting.network.packet.host.HostBackupPacket;
import net.rezxis.mchosting.network.packet.host.HostCreateServer;
import net.rezxis.mchosting.network.packet.host.HostDeleteServer;
import net.rezxis.mchosting.network.packet.host.HostWorldPacket;
import net.rezxis.mchosting.network.packet.host.HostWorldPacket.Action;
import net.rezxis.mchosting.network.packet.sync.SyncServerCreated;

public class ServerFileManager {

	private static Gson gson = new Gson();
	
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
	
	public static void deleteServer(String json) {
		HostDeleteServer packet = gson.fromJson(json, HostDeleteServer.class);
		try {
			FileUtils.forceDelete(new File("servers/"+packet.id));
			System.out.println(packet.id+"Server was deleted");
		} catch (IOException e) {
			e.printStackTrace();
		}
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

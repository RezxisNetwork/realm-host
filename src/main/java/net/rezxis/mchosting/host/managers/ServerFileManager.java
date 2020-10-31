package net.rezxis.mchosting.host.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.zip.ZipUtil;

import com.google.gson.Gson;

import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.internal.DBBackup;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.server.DBBackupPluginLink;
import net.rezxis.mchosting.database.object.server.DBBackupShopItemLink;
import net.rezxis.mchosting.database.object.server.DBPlugin;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.object.server.DBServer.GameType;
import net.rezxis.mchosting.database.object.server.DBServerPluginLink;
import net.rezxis.mchosting.database.object.server.DBShopItem;
import net.rezxis.mchosting.database.object.server.ServerStatus;
import net.rezxis.mchosting.database.object.server.Version;
import net.rezxis.mchosting.host.HostServer;
import net.rezxis.mchosting.host.game.ServerFileUtil;
import net.rezxis.mchosting.network.packet.enums.BackupAction;
import net.rezxis.mchosting.network.packet.host.HostBackupPacket;
import net.rezxis.mchosting.network.packet.host.HostCreateServer;
import net.rezxis.mchosting.network.packet.host.HostDeleteServer;
import net.rezxis.mchosting.network.packet.host.HostWorldPacket;
import net.rezxis.mchosting.network.packet.host.HostWorldPacket.Action;
import net.rezxis.mchosting.network.packet.sync.SyncPlayerMessagePacket;
import net.rezxis.mchosting.network.packet.sync.SyncServerCreated;
import net.rezxis.utils.WebAPI;

public class ServerFileManager {

	private static Gson gson = new Gson();
	
	public static void createServer(String json) {
		HostCreateServer createPacket = gson.fromJson(json, HostCreateServer.class);
		if (Tables.getSTable().get(UUID.fromString(createPacket.player)) != null) {
			System.out.println("A server to tried to create has already depolyed");
			return;
		}
		DBServer server = new DBServer(-1, createPacket.displayName,
				UUID.fromString(createPacket.player), -1, "", 
				-1,ServerStatus.STOP,createPacket.world, HostServer.props.HOST_ID,
				//"",true,true,"EMERALD_BLOCK", new DBShop(new ArrayList<>()),0,GameType.valueOf(createPacket.stype), "", "", "");
				"",true,true,"EMERALD_BLOCK",0,GameType.valueOf(createPacket.stype), "", "", "",Version.valueOf(createPacket.version));
		DBPlayer player = Tables.getPTable().get(UUID.fromString(createPacket.player));
		Tables.getSTable().insert(server);
		if (server.getType() == GameType.CUSTOM) {
			SyncServerCreated packet = new SyncServerCreated(server.getOwner().toString());
			HostServer.client.send(gson.toJson(packet));
			return;
		}
		DBServerPluginLink link = new DBServerPluginLink(-1,server.getId(), "ViaVersion", Tables.getPlTable().get("ViaVersion").get(0).getId(),true,false);
		Tables.getSplTable().insert(link);
		server.update();
		new Thread(()->{
			ServerFileUtil.generateServerFile(server,player);
			if (server.getType() == GameType.CUSTOM) {
				try {
					FileUtils.copyFile(new File("spigot/spigot.jar"), new File("servers/"+server.getId()+"/spigot.jar"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
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
		DBServer server = Tables.getSTable().get(UUID.fromString(uuid));
		File cacheDir = new File("cache");
		if (!cacheDir.exists())
			cacheDir.mkdirs();
		File cache = new File(cacheDir, uuid+"world.zip");
		try {
			WebAPI.downloadWorld(cache, secret, uuid);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		if (!isZipFile(cache)) {
			return;
		}
		try {
			File tmp = new File("cache/"+server.getId()+"/world");
			if (tmp.exists())
				FileUtils.forceDelete(tmp);
			tmp.mkdirs();
			ZipUtil.unpack(cache, tmp);
			String f = checkBlackListed(tmp);
			if (f != null) {
				System.out.println(uuid+" : blacklisted file was detected!");
				HostServer.client.send(gson.toJson(new SyncPlayerMessagePacket(UUID.fromString(uuid),"&c禁止された拡張子のfileが検出されました。")));
				return;
			}
			
			File dest = new File("servers/"+server.getId()+"/world");
			FileUtils.forceDelete(dest);
			FileUtils.moveDirectory(tmp, dest);
			
			cache.delete();
			System.out.println("world upload takes "+(System.currentTimeMillis()-time)+"ms");
			HostServer.client.send(gson.toJson(new SyncPlayerMessagePacket(UUID.fromString(uuid),"&aWorldがアップロードされました。 所要時間"+(System.currentTimeMillis()-time)+"ms")));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static String checkBlackListed(File t) {
		String[] ex = new String[] {"class","jar","zip"};
		for (File file : t.listFiles()) {
			if (file.isDirectory()) {
				return checkBlackListed(file);
			} else {
				for (String s : ex) {
					if (file.getName().toLowerCase().endsWith(s)) {
						return file.getName();
					}
				}
			}
		}
		return null;
	}
	
	@SuppressWarnings("resource")
	private static boolean isZipFile(File f) {
        if (f.isDirectory())
            return false;
        try {
        	new ZipFile(f);
            return true;
        } catch (ZipException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
	
	public static void backup(String json) {
		HostBackupPacket packet = gson.fromJson(json, HostBackupPacket.class);
		DBServer server = Tables.getSTable().get(UUID.fromString(packet.owner));
		if (server == null) {
			System.out.print("received a backup request from no server.");
			return;
		}
		try {
			if (packet.action == BackupAction.TAKE) {
				if (!new File("backups").exists()) {
					new File("backups").mkdirs();
				}
				server.setStatus(ServerStatus.BACKUP);
				server.update();
				DBBackup obj = new DBBackup(-1, packet.owner, packet.value.get("name"), new Date());
				Tables.getBTable().insert(obj);
				for (DBServerPluginLink pLink : Tables.getSplTable().getAllByServer(server.getId())) {
					if (pLink.isEnabled()) {
						DBBackupPluginLink bLink = new DBBackupPluginLink(-1, obj.getId(), pLink.getDBPlugin().getId());
						Tables.getBplTable().insert(bLink);
					}
				}
				for (DBShopItem item : Tables.getSiTable().getShopItems(server.getId())) {
					new DBBackupShopItemLink(-1, obj.getId(), item.getName(), item.getItemType(), item.getCmd(), item.getPrice(), 0).insert();
				}
				File dest = new File("backups/"+obj.getId()+".zip");
				try {
					ZipUtil.pack(new File("servers/"+server.getId()), dest);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				/*try {
					BackBlazeAPI.upload("rezxis-backup", obj.getId()+".zip", dest);
					FileUtils.forceDelete(dest);
				} catch (B2Exception | IOException e) {
					e.printStackTrace();
				}*/
				server.sync();
				server.setStatus(ServerStatus.STOP);
				server.update();
			} else if (packet.action == BackupAction.DELETE) {
				DBBackup obj = Tables.getBTable().getBackupFromID(Integer.valueOf(packet.value.get("id")));
				for (DBBackupPluginLink link : Tables.getBplTable().getAllByBackup(obj.getId())) {
					Tables.getBplTable().delete(link);
				}
				for (DBBackupShopItemLink link : Tables.getBsiTable().getShopItems(obj.getId())) {
					link.delete();
				}
				try {
					FileUtils.forceDelete(new File("backups/"+server.getId()+".zip"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				/*try {
					BackBlazeAPI.delete("rezxis-backup", obj.getId()+".zip");
				} catch (B2Exception e) {
					e.printStackTrace();
				}*/
				Tables.getBTable().delete(obj);
			} else if (packet.action == BackupAction.PATCH) {
				DBBackup obj = Tables.getBTable().getBackupFromID(Integer.valueOf(packet.value.get("id")));
				server.setStatus(ServerStatus.BACKUP);
				ArrayList<DBBackupPluginLink> blinks = Tables.getBplTable().getAllByBackup(obj.getId());
				ArrayList<DBServerPluginLink> slinks = Tables.getSplTable().getAllByServer(server.getId());
				slinks.forEach((slink) -> {slink.setEnabled(false);slink.update();});
				for (DBBackupPluginLink blink : blinks) {
					DBPlugin plugin = Tables.getPlTable().getPluginById(blink.getPlugin());
					DBServerPluginLink slink = Tables.getSplTable().getLink(server.getId(), plugin.getName());
					if (slink != null) {
						slink.setEnabled(true);
						slink.setPlugin(plugin.getId());
						slink.update();
					} else {
						slink = new DBServerPluginLink(-1, server.getId(), plugin.getName(), plugin.getId(), true, false);
						Tables.getSplTable().insert(slink);
					}
				}
				DBPlayer player = Tables.getPTable().get(server.getOwner());
				for (DBShopItem item : Tables.getSiTable().getShopItems(server.getId())) {
					player.addCoin(item.getEarned());
					item.delete();
				}
				player.update();
				for (DBBackupShopItemLink link : Tables.getBsiTable().getShopItems(obj.getId())) {
					new DBShopItem(-1, server.getId(), link.getName(), link.getItemType(), link.getCmd(), link.getPrice(), 0).insert();
				}
				server.update();
				File sFile = new File("servers/"+server.getId());
				sFile.delete();
				ZipUtil.unpack(new File("backups/"+obj.getId()+".zip"), sFile);
				/*File zip = new File("backups/"+obj.getId()+".zip");
				try {
					BackBlazeAPI.download("rezxis-backup", obj.getId()+".zip", zip);
					File sFile = new File("servers/"+server.getId());
					sFile.delete();
					ZipUtil.unpack(zip, sFile);
					FileUtils.forceDelete(zip);
				} catch (B2Exception | IOException e) {
					e.printStackTrace();
				}*/
				
				server.setStatus(ServerStatus.STOP);
				server.update();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			server.setStatus(ServerStatus.STOP);
			server.update();
			HostServer.client.send(gson.toJson(new SyncPlayerMessagePacket(server.getOwner(),"&a内部エラーが発生したため、バックアップが失敗しました。ticketで連絡してください。")));
		}
	}
	
	private static void downloadWorld(HostWorldPacket packet) {
		
	}
}

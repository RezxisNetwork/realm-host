package net.rezxis.mchosting.host;

import java.util.UUID;

import com.google.gson.Gson;

import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.object.server.DBServer.GameType;
import net.rezxis.mchosting.host.managers.ServerFileManager;
import net.rezxis.mchosting.host.managers.games.CustomDockerManager;
import net.rezxis.mchosting.host.managers.games.DockerManager;
import net.rezxis.mchosting.host.managers.games.GameManager;
import net.rezxis.mchosting.network.packet.Packet;
import net.rezxis.mchosting.network.packet.PacketType;
import net.rezxis.mchosting.network.packet.ServerType;
import net.rezxis.mchosting.network.packet.host.*;
import net.rezxis.mchosting.network.packet.sync.SyncThirdPartyPacket;
import net.rezxis.mchosting.network.packet.sync.SyncThirdPartyPacket.Action;

public class WorkerThread extends Thread {

	private static Gson gson = new Gson();
	public static DockerManager dMgr;
	public static CustomDockerManager cMgr;
	public String message;
	
	public WorkerThread(String json) {
		this.message = json;
	}
	
	//received=>getStatus(DBServer,DBPlayer)->query
	
	public void run() {
		Packet packet = gson.fromJson(message, Packet.class);
		PacketType type = packet.type;
		if (packet.dest != ServerType.HOST) {
			System.out.println("packet dest is not good.");
			System.out.println(message);
			System.out.println("-----------------------");
			return;
		}
		System.out.println("Received : "+message);
		if (type == PacketType.CreateServer) {
			ServerFileManager.createServer(message);
			//GameManager.createServer(message);
		} else if (type == PacketType.StartServer) {
			HostStartServer sp = gson.fromJson(message, HostStartServer.class);
			DBServer server = Tables.getSTable().get(UUID.fromString(sp.player));
			if (server.getType() == GameType.NORMAL) {
				dMgr.start(server);
			} else {
				cMgr.start(server);
			}
		} else if (type == PacketType.StopServer) {
			HostStopServer sp = gson.fromJson(message, HostStopServer.class);
			DBServer server = Tables.getSTable().get(UUID.fromString(sp.player));
			if (server.getType() == GameType.NORMAL) {
				dMgr.kill(server);
			} else {
				cMgr.kill(server);
			}
		} else if (type == PacketType.ServerStopped) {
			HostStoppedServer sp = gson.fromJson(message, HostStoppedServer.class);
			DBServer server = Tables.getSTable().get(UUID.fromString(sp.player));
			if (server.getType() == GameType.NORMAL) {
				dMgr.stopped(server);
			} else {
				cMgr.stopped(server);
			}
		} else if (type == PacketType.RebootServer) {
			HostRebootServer sp = gson.fromJson(message, HostRebootServer.class);
			DBServer server = Tables.getSTable().getByID(sp.id);
			if (server.getType() == GameType.NORMAL) {
				dMgr.reboot(server);
			} else {
				cMgr.reboot(server);
			}
		} else if (type == PacketType.DeleteServer) {
			ServerFileManager.deleteServer(message);
		} else if (type == PacketType.World) {
			ServerFileManager.world(message);
		} else if (type == PacketType.Backup) {
			ServerFileManager.backup(message);
		}
	}
}

package net.rezxis.mchosting.host;

import com.google.gson.Gson;

import net.rezxis.mchosting.host.managers.DockerManager;
import net.rezxis.mchosting.host.managers.GameManager;
import net.rezxis.mchosting.host.managers.ServerFileManager;
import net.rezxis.mchosting.network.packet.Packet;
import net.rezxis.mchosting.network.packet.PacketType;
import net.rezxis.mchosting.network.packet.ServerType;
import net.rezxis.mchosting.network.packet.host.HostWorldPacket;

public class WorkerThread extends Thread {

	private static Gson gson = new Gson();
	public String message;
	
	public WorkerThread(String json) {
		this.message = json;
	}
	
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
			DockerManager.start(message);
			//GameManager.startServer(message);
		} else if (type == PacketType.StopServer) {
			DockerManager.kill(message);
			//GameManager.forceStop(message);
		} else if (type == PacketType.ServerStopped) {
			DockerManager.stopped(message);
			//GameManager.stoppedServer();
		} else if (type == PacketType.RebootServer) {
			DockerManager.reboot(message);
			//GameManager.rebootServer(message);
		} else if (type == PacketType.DeleteServer) {
			ServerFileManager.deleteServer(message);
			//GameManager.deleteServer(message);
		} else if (type == PacketType.World) {
			ServerFileManager.world(message);
		} else if (type == PacketType.Backup) {
			ServerFileManager.backup(message);
		}
	}
}

package net.rezxis.mchosting.host;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.Gson;

import net.rezxis.mchosting.network.ClientHandler;
import net.rezxis.mchosting.network.packet.ServerType;
import net.rezxis.mchosting.network.packet.sync.SyncAuthSocketPacket;

public class WSClientHandler implements ClientHandler {

	public static Gson gson = new Gson();
	public static ExecutorService pool = null;
	
	@Override
	public void onMessage(String message) {
		pool.submit(new WorkerThread(message));
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		System.out.println("closed / code : "+code+" / reason : "+reason+" / remote : "+remote);
	}

	@Override
	public void onMessage(ByteBuffer buffer) {
	}

	@Override
	public void onError(Exception ex) {
		ex.printStackTrace();
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		pool = Executors.newFixedThreadPool(5);
		HashMap<String,String> values = new HashMap<>();
		values.put("id", String.valueOf(HostServer.props.HOST_ID));
		SyncAuthSocketPacket packet = new SyncAuthSocketPacket(ServerType.HOST, values);
		HostServer.client.send(gson.toJson(packet));
	}
}

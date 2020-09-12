package net.rezxis.mchosting.host.managers.anni;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.java_websocket.WebSocket;

import com.google.gson.Gson;

import net.rezxis.mchosting.network.packet.host.HostAnniServerStatusSigns;

public class AnniManager {

	private static final CopyOnWriteArrayList<StatusSignInfo> servers = new CopyOnWriteArrayList<StatusSignInfo>();
	private static Gson gson = new Gson();
	
	public static void packetAnniServerStatusSigns(String packet) {
		HostAnniServerStatusSigns pack = gson.fromJson(packet, HostAnniServerStatusSigns.class);
		StatusSignInfo target = search(pack.getServerName());
		if(target==null) {
			getServerList().add(new StatusSignInfo(pack.getServerName(),pack.isJoinable(),pack.getMaxPlayers(),pack.getOnlinePlayers(),true,pack.getIp(),pack.getPort(),pack.getIcon(),pack.getLine1(),pack.getLine2(),pack.getLine3(),pack.getLine4(),pack.getLastUpdated()));
		}else {
			target.update(pack.getServerName(),pack.isJoinable(),pack.getMaxPlayers(),pack.getOnlinePlayers(),true,pack.getIp(),pack.getPort(),pack.getIcon(),pack.getLine1(),pack.getLine2(),pack.getLine3(),pack.getLine4(),pack.getLastUpdated());
		}
	}
	
	public static CopyOnWriteArrayList<StatusSignInfo> getServerList() {
		return servers;
	}

	public static StatusSignInfo search(int port) {
		for(StatusSignInfo e:getServerList()) {
			if(e.getPort()==port) {
				return e;
			}
		}
		return null;
	}


	public static StatusSignInfo search(String s) {
		for(StatusSignInfo e:getServerList()) {
			if(e.getServerName().equals(s)) {
				return e;
			}
		}
		return null;
	}

	public static ArrayList<StatusSignInfo> searchContainsName(String fillter) {
		ArrayList<StatusSignInfo> list=new ArrayList<StatusSignInfo>();
		for(StatusSignInfo e:getServerList()) {
			if(e.getServerName().contains(fillter)) {
				list.add(e);
			}
		}
		return list;
	}
	public static ArrayList<StatusSignInfo> searchContainsNameAndOnline(String fillter) {
		ArrayList<StatusSignInfo> list=new ArrayList<StatusSignInfo>();
		for(StatusSignInfo e:getServerList()) {
			if(e.getServerName().contains(fillter)&&e.isOnline()) {
				list.add(e);
			}
		}
		return list;
	}

	public static ArrayList<StatusSignInfo> searchNonConnectedServer(String fillter) {

		ArrayList<StatusSignInfo> list=new ArrayList<StatusSignInfo>();
		for(StatusSignInfo e:searchContainsName(fillter)) {
			if(!e.isConnected()) {
				list.add(e);
			}
		}
		return list;
	}

	public static StatusSignInfo findOffLine() {
		for(StatusSignInfo e:getServerList()) {
			if(!e.isOnline()) {
				return e;
			}
		}
		return null;
	}
}

package net.rezxis.mchosting.host.managers.anni;

import java.util.Date;

import org.java_websocket.WebSocket;

public class StatusSignInfo {


	private String serverName;
	private boolean joinable;
	private int maxPlayers;
	private int onlinePlayers;
	private boolean online;
	private String ip;
	private int port;
	private String icon;
	private String line1;
	private String line2;
	private String line3;
	private String line4;
	private Date lastUpdated;

	private boolean isConnected;


	public StatusSignInfo(String serverName, boolean joinable, int maxPlayers, int onlinePlayers, boolean online,
			String ip, int port, String icon, String line1, String line2, String line3, String line4, Date lastUpdated) {
		update( serverName,  joinable,  maxPlayers,  onlinePlayers,  online,
				 ip,  port,  icon,  line1,  line2,  line3,  line4,  lastUpdated);

	}


	public void update(String serverName, boolean joinable, int maxPlayers, int onlinePlayers, boolean online,String ip, int port, String icon, String line1, String line2, String line3, String line4, Date lastUpdated) {
		this.serverName = serverName;
		this.joinable = joinable;
		this.maxPlayers = maxPlayers;
		this.onlinePlayers = onlinePlayers;
		this.online = online;
		this.ip = ip;
		this.port = port;
		this.icon = icon;
		this.line1 = line1;
		this.line2 = line2;
		this.line3 = line3;
		this.line4 = line4;
		this.lastUpdated = lastUpdated;
		isConnected=true;

	}



	public StatusSignInfo(String serverName2, int port2) {
		this.serverName=serverName2;
		this.port=port2;

		isConnected=false;

	}


	public String getServerName() {
		return serverName;
	}


	public boolean isJoinable() {
		return joinable;
	}


	public int getMaxPlayers() {
		return maxPlayers;
	}


	public int getOnlinePlayers() {
		return onlinePlayers;
	}


	public boolean isOnline() {
		return online;
	}


	public String getIp() {
		return ip;
	}


	public int getPort() {
		return port;
	}


	public String getIcon() {
		return icon;
	}


	public String getLine1() {
		return line1;
	}


	public String getLine2() {
		return line2;
	}


	public String getLine3() {
		return line3;
	}


	public String getLine4() {
		return line4;
	}


	public Date getLastUpdated() {
		return lastUpdated;
	}


	public void setOnline(boolean b) {

		this.online=b;

	}


	public void setJoinable(boolean b) {
		this.joinable=b;
	}


	public boolean isConnected() {
		return isConnected;
	}


	public void setConnected(boolean b) {
		this.isConnected=b;
	}
}
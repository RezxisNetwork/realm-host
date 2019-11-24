package net.rezxis.mchosting.host;

import java.net.URI;

import net.rezxis.mchosting.databse.Database;
import net.rezxis.mchosting.databse.tables.PlayersTable;
import net.rezxis.mchosting.databse.tables.PluginsTable;
import net.rezxis.mchosting.databse.tables.ServersTable;
import net.rezxis.mchosting.network.WSClient;

public class HostServer {

	public static Props props;
	public static WSClient client;
	public static ServersTable sTable;
	public static PluginsTable plTable;
	public static PlayersTable psTable;
	
	public static void main(String[] args) {
		props = new Props("host.propertis");
		Database.init();
		sTable = new ServersTable();
		plTable = new PluginsTable();
		psTable = new PlayersTable();
		try {
			client = new WSClient(new URI(props.SYNC_ADDRESS),  new WSClientHandler());
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("failed to init websocket.");
			return;
		}
		client.connect();
		
	}
}

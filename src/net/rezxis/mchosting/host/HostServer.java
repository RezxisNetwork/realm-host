package net.rezxis.mchosting.host;

import java.net.URI;

import net.rezxis.mchosting.database.Database;
import net.rezxis.mchosting.database.tables.BackupsTable;
import net.rezxis.mchosting.database.tables.PlayersTable;
import net.rezxis.mchosting.database.tables.PluginsTable;
import net.rezxis.mchosting.database.tables.ServersTable;
import net.rezxis.mchosting.host.managers.DockerManager;
import net.rezxis.mchosting.network.WSClient;

public class HostServer {

	public static Props props;
	public static WSClient client;
	public static ServersTable sTable;
	public static PluginsTable plTable;
	public static PlayersTable psTable;
	public static BackupsTable bTable;
	
	public static void main(String[] args) {
		props = new Props("host.propertis");
		Database.init();
		sTable = new ServersTable();
		plTable = new PluginsTable();
		psTable = new PlayersTable();
		bTable = new BackupsTable();
		try {
			DockerManager.connect(props.DOCKER_ADDRESS, props.DOCKER_PORT);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("couldn't connect to docker.");
			return;
		}
		try {
			client = new WSClient(new URI("ws://"+props.SYNC_ADDRESS+":"+props.SYNC_PORT),  new WSClientHandler());
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("failed to init websocket.");
			return;
		}
		client.connect();
	}
}

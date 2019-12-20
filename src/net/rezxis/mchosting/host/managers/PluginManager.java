package net.rezxis.mchosting.host.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import net.rezxis.mchosting.database.DBPlugin;
import net.rezxis.mchosting.database.DBServer;
import net.rezxis.mchosting.database.Database;
import net.rezxis.mchosting.host.HostServer;

public class PluginManager {

	public static final File pluginFolder = new File("plugins");
	
	public static void checkPlugins(DBServer server) throws Exception {
		HashMap<String,DBPlugin> plugins = HostServer.plTable.getPlugins();
		DBPlugin rezxisMC = plugins.get("RezxisMCHosting");
		server.sync();
		File f = new File("servers/"+server.getID()+"/plugins/");
		File db = new File(f,"database.propertis");
		File sync = new File(f,"hosting.propertis");
		ArrayList<DBPlugin> list = new ArrayList<DBPlugin>(plugins.values());
		list.remove(rezxisMC);
		for (String s : server.getPlugins()) {
			DBPlugin p = plugins.get(s);
			list.remove(p);
		}
		for (File file : f.listFiles()) {
			for (DBPlugin p : list) {
				if (file.getName().equalsIgnoreCase(p.getJarName())) {
					FileUtils.forceDelete(file);
				}
				if (file.getName().equalsIgnoreCase(p.getName())) {
					if (file.exists())
						FileUtils.forceDelete(file);
				}
			}
		}
		check(server, rezxisMC);
		for (String p : server.getPlugins()) {
			if (plugins.containsKey(p))
				check(server, plugins.get(p));
		}
		db(db);
		sync(sync);
		
	}
	
	private static void sync(File file) throws Exception {
		if (!file.exists()) {
			file.createNewFile();
		}
		PrintWriter pw = new PrintWriter(file);
		pw.println("sync_address="+"ws://"+HostServer.props.DOCKER_GATEWAY+":"+HostServer.props.SYNC_PORT);
		pw.close();
	}
	private static void db(File file) throws Exception {
		PrintWriter pw = new PrintWriter(file);
		pw.println("db_host="+HostServer.props.DOCKER_GATEWAY);
		pw.println("db_user="+Database.getProps().DB_USER);
		pw.println("db_pass="+Database.getProps().DB_PASS);
		pw.println("db_port="+Database.getProps().DB_PORT);
		pw.println("db_name="+Database.getProps().DB_NAME);
		pw.close();
	}
	
	private static void check(DBServer server, DBPlugin plugin) throws Exception {
		File plugins = new File("servers/"+server.getID()+"/plugins/");//server plugins folder
		File pFile = new File(plugins,plugin.getJarName());//dest plugin
		File sFile = new File(pluginFolder, plugin.getJarName());//source plugin
		if (!pFile.exists()) {//not copied plugin
			FileUtils.copyFile(sFile, pFile);
		}
		FileInputStream fis = new FileInputStream(sFile);
		String srcHash = DigestUtils.md5Hex(fis);
		fis.close();
		fis = new FileInputStream(pFile);
		String destHash = DigestUtils.md5Hex(fis);
		fis.close();
		if (!srcHash.equals(destHash)) {//not match hash (updated or cracked)
			pFile.delete();
			FileUtils.copyFile(sFile, pFile);
		}
	}
}

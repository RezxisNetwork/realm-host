package net.rezxis.mchosting.host.managers;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.server.DBPlugin;
import net.rezxis.mchosting.database.object.server.DBServer;

public class PluginManager {

	public static final File pluginFolder = new File("plugins");
	
	public static void checkPlugins(DBServer server) throws Exception {
		HashMap<String,DBPlugin> plugins = Tables.getPlTable().getPlugins();
		DBPlugin rezxisMC = plugins.get("RezxisMCHosting");
		server.sync();
		File f = new File("servers/"+server.getId()+"/plugins/");
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
				if (file.getName().endsWith(".jar")) {
					if (file.getName().equalsIgnoreCase(p.getJarName())) {
						if (file.exists())
							FileUtils.forceDelete(file);
					}
					if (file.getName().equalsIgnoreCase(p.getName())) {
						if (file.exists())
							FileUtils.forceDelete(file);
					}
				}
			}
		}
		check(server, rezxisMC);
		for (String p : server.getPlugins()) {
			if (plugins.containsKey(p))
				check(server, plugins.get(p));
		}
		if (db.exists())
			db.delete();
		if (sync.exists())
			sync.delete();
		
	}
	
	private static void check(DBServer server, DBPlugin plugin) throws Exception {
		File plugins = new File("servers/"+server.getId()+"/plugins/");//server plugins folder
		File pFile = new File(plugins,plugin.getJarName());//dest plugin
		File sFile = new File(pluginFolder, plugin.getJarName());//source plugin
		if (!pFile.exists()) {//not copied plugin
			FileUtils.copyFile(sFile, pFile);
			return;
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

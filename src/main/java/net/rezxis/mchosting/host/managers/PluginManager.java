package net.rezxis.mchosting.host.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.server.DBPlugin;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.object.server.DBServerPluginLink;

public class PluginManager {

	public static final File pluginFolder = new File("plugins");
	public static String[] forced = new String[] {"RezxisMCHosting","RezxisSQL"};
	
	public static void checkPlugins(DBServer server) throws Exception {
		File source = new File("plugins/");
		File plugins = new File("servers/"+server.getId()+"/plugins/");
		for (DBServerPluginLink link : Tables.getSplTable().getAllByServer(server.getId())) {
			DBPlugin plugin = link.getDBPlugin();
			for (String s : forced)
				if (plugin.getName().equalsIgnoreCase(s)) {
					link.setEnabled(true);
					link.setLastEnabled(true);
				}
			if (link.isEnabled()) {
				if (!link.isLastEnabled()) {
					FileUtils.copyFile(new File(source, plugin.getJarName()), new File(plugins, plugin.getJarName()));
				} else {
					checkPlugin(source, plugins, plugin, server);
				}
			} else {
				if (link.isLastEnabled()) {
					FileUtils.forceDelete(new File(plugins, plugin.getJarName()));
				}
			}
			link.setLastEnabled(link.isEnabled());
			link.update();
		}
		initDB(plugins);
	}
	
	private static void checkPlugin(File s, File p, DBPlugin plugin, DBServer server) throws IOException {
		File source = new File(s, plugin.getJarName());
		File dest = new File(s, plugin.getJarName());
		if (!dest.exists()) {
			FileUtils.copyFile(source, dest);
			return;
		}
		if (!DigestUtils.md5Hex(new FileInputStream(source)).equals(DigestUtils.md5Hex(new FileInputStream(dest)))) {
			FileUtils.copyFile(source, dest);
		}
	}
	
	private static void initDB(File plugins) throws IOException {
		File rezxisSQLDir = new File(plugins, "RezxisSQLPlugin");
		if (!rezxisSQLDir.exists()) {
			rezxisSQLDir.mkdirs();
		}
		File sqlConf = new File(rezxisSQLDir, "database.yml");
		if (!sqlConf.exists()) {
			sqlConf.createNewFile();
		}
		FileUtils.copyFile(new File("base/plugins/RezxisSQLPlugin/database.yml"), sqlConf);
	}
}

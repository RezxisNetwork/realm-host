package net.rezxis.mchosting.host.game;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.host.HostServer;

public class ServerFileUtil {

	public static File generateServerFile(DBServer server, DBPlayer player)  {
		File base = new File(HostServer.props.BASE_DIR);
		System.out.println(base.getAbsolutePath());
		File gameDir = new File("servers/"+server.getId());

		if (!gameDir.exists()) {
			gameDir.mkdirs();
		}else {
			deleteDirectory(gameDir);
			gameDir.mkdirs();
		}

		copyDirectory(base,gameDir);
		MCProperties mcprop = new MCProperties(server,player);
		try {
			mcprop.generateFile(gameDir);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return gameDir;
	}

	private static void copyDirectory(File in, File target) {
		try {
			FileUtils.copyDirectory(in, target);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void deleteDirectory(File file) {
		try {
			recursiveDeleteFile(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void recursiveDeleteFile(File file) throws Exception {
		if (!file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] arrayOfFile;
			int j = (arrayOfFile = file.listFiles()).length;
			for (int i = 0; i < j; i++) {
				File child = arrayOfFile[i];
				recursiveDeleteFile(child);
			}
		}
		file.delete();
	}

}

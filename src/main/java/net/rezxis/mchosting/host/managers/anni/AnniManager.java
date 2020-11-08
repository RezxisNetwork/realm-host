package net.rezxis.mchosting.host.managers.anni;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AnniManager {

	private static final File anniRoot = new File("/servers/realm2/anni/");
	private static final AnniPlugin[] plugins = new AnniPlugin[] {AnniPlugin.ANNIHILATION,AnniPlugin.ANTILOGGER,AnniPlugin.BONUSMANAGER,AnniPlugin.CHATMANAGER,AnniPlugin.CORE,AnniPlugin.MINEZSTATUS,
			AnniPlugin.PARTIES,AnniPlugin.PREFERENCES,AnniPlugin.RANKS,AnniPlugin.SERVERSTATUSSIGN,AnniPlugin.XP,AnniPlugin.SOULBOUND,AnniPlugin.REZXISSQLPLUGIN};
	
	public static void start(int port) {
		String serverName = "ANNI_"+port;
		File gameDir=generateServerFile(serverName, port, "172.18.0."+(1+port));
		initPlugins(new File(gameDir,"plugins"));
		AnniGameMaker gameMaker = new AnniGameMaker(serverName, new File(gameDir,"spigot.jar"), gameDir, port);
		gameMaker.setPort(port);
		gameMaker.build();
		gameMaker.start();
		System.out.println("Started game : "+serverName + " on "+port);
	}
	
	private static void initPlugins(File pdir) {
		if (!pdir.exists()) {
			pdir.mkdirs();
		}
		OkHttpClient oClient = new OkHttpClient.Builder().build();
		for (AnniPlugin plugin : plugins) {
			try {
				long time = System.currentTimeMillis();
				Response res = oClient.newCall(new Request.Builder().url(plugin.getUrl()).get().addHeader("Authorization", Credentials.basic("rezxis", "11edb810d81cf6741b74f245ee58e8a81f")).build()).execute();
				FileUtils.copyToFile(res.body().byteStream(), new File(pdir,plugin.getJarName()));
				res.close();
				System.out.println("downloaded "+plugin.name()+" in "+(System.currentTimeMillis()-time)+"ms");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public static File generateServerFile(String serverName,int port, String ip)  {
		File base = new File(anniRoot,"base");
		File gameDir = new File(anniRoot,"games/"+serverName);

		if (!gameDir.exists()) {
			gameDir.mkdirs();
		}else {
			try {
				FileUtils.forceDelete(gameDir);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			gameDir.mkdirs();
		}

		try {
			FileUtils.copyDirectory(base,gameDir);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		AnniMCProperties mcprop=new AnniMCProperties(serverName, ip, port);
		try {
			mcprop.generateFile(gameDir);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return gameDir;
	}
}

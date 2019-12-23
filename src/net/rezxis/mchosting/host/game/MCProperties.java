package net.rezxis.mchosting.host.game;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.server.DBServer;

public class MCProperties {
    final Properties properties = new Properties();
    public MCProperties(DBServer server, DBPlayer player) {
    	if (server.getWorld().equalsIgnoreCase("void")) {
    		properties.setProperty("generator-settings", "0;0;0;");
    	} else {
    		properties.setProperty("generator-settings", "");
    	}
    	properties.setProperty("op-permission-level", "4");
    	properties.setProperty("allow-nether", "true");
    	properties.setProperty("level-name", "world");
    	properties.setProperty("enable-query", "false");
    	properties.setProperty("allow-flight", "true");
    	properties.setProperty("prevent-proxy-connections", "false");
    	properties.setProperty("server-port", String.valueOf(server.getPort()));
    	properties.setProperty("max-world-size", "29999984");
    	if (server.getWorld().equalsIgnoreCase("default"))
    		properties.setProperty("level-type", "DEFAULT");
    	if (server.getWorld().equalsIgnoreCase("flat"))
    		properties.setProperty("level-type", "FLAT");
    	if (server.getWorld().equalsIgnoreCase("void"))
    		properties.setProperty("level-type", "FLAT");
    	properties.setProperty("enable-rcon","false");
    	properties.setProperty("level-seed", "");
    	properties.setProperty("force-gamemode", "false");
    	properties.setProperty("server-ip", "");
    	properties.setProperty("network-compression-threshold", "256");
    	properties.setProperty("max-build-height", "256");
    	properties.setProperty("spawn-npcs", "true");
    	properties.setProperty("white-list", "false");
    	properties.setProperty("spawn-animals", "true");
    	properties.setProperty("hardcore", "false");
    	properties.setProperty("snooper-enabled", "true");
    	properties.setProperty("resource-pack-sha1", "");
    	properties.setProperty("online-mode", "false");
    	properties.put("resource-pack", "");
    	properties.put("pvp", "true");
    	properties.setProperty("difficulty", "1");
    	properties.setProperty("enable-command-block", String.valueOf(server.isCmd()));
    	properties.setProperty("gamemode", "0");
    	properties.setProperty("player-idle-timeout", "0");
    	properties.setProperty("max-players", String.valueOf(player.getRank().getMaxPlayers()));
    	properties.setProperty("spawn-monsters", "true");
    	properties.setProperty("generate-structures", "true");
    	properties.setProperty("view-distance", "10");
    	properties.setProperty("motd",server.getDisplayName());
    	properties.setProperty("spawn-protection", ""+0);
    	properties.setProperty("server-name", server.getDisplayName());
    }

    public void generateFile(File gameDir) throws FileNotFoundException, IOException {
    	FileOutputStream fos = new FileOutputStream(new File(gameDir,"server.properties"));
        properties.store(fos, "Rezxis Realms");
        fos.close();
        
    }
}

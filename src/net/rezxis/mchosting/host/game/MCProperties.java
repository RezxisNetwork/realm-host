package net.rezxis.mchosting.host.game;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class MCProperties {
    final Properties properties = new Properties();
    public MCProperties(String name,int port, String type, boolean cmd) {
    	properties.setProperty("spawn-protection", ""+0);
    	properties.setProperty("max-tick-time", ""+60000);
    	properties.setProperty("query.port", ""+25567);
    	properties.setProperty("server-name", name);
    	if (type.equalsIgnoreCase("void")) {
    		properties.setProperty("generator-settings", "0;0;0;");
    	} else {
    		properties.setProperty("generator-settings", "");
    	}
    	properties.setProperty("force-gamemode", "true");
    	properties.setProperty("allow-nether", "false");
    	properties.setProperty("gamemode", "0");
    	properties.setProperty("broadcast-console-to-ops", "true");
    	properties.setProperty("enable-query", "false");
    	properties.setProperty("player-idle-timeout", "0");
    	properties.setProperty("difficulty", "0");
    	properties.setProperty("spawn-monsters", "true");
    	properties.setProperty("op-permission-level", "4");
    	properties.setProperty("resource-pack-hash", "");
    	properties.setProperty("announce-player-achievements", "false");
    	properties.setProperty("pvp", "true");
    	properties.setProperty("snooper-enabled", "false");
    	if (type.equalsIgnoreCase("default"))
    		properties.setProperty("level-type", "DEFAULT");
    	if (type.equalsIgnoreCase("flat"))
    		properties.setProperty("level-type", "FLAT");
    	if (type.equalsIgnoreCase("void"))
    		properties.setProperty("level-type", "FLAT");
    	properties.setProperty("enable-command-block", String.valueOf(cmd));
    	properties.setProperty("max-players", "10");
    	properties.setProperty("network-compression-threshold", "-1");
    	properties.setProperty("resource-pack-sha1", "");
    	properties.setProperty("max-world-size", "29999");
    	properties.setProperty("server-port", ""+port);
    	properties.setProperty("debug", "false");
    	properties.setProperty("server-ip", "");
    	properties.setProperty("spawn-npcs", "true");
    	properties.setProperty("allow-flight", "true");
    	properties.setProperty("level-name", "world");
    	properties.setProperty("view-distance", "16");
    	properties.setProperty("resource-pack", "");
    	properties.setProperty("spawn-animals", "true");
    	properties.setProperty("white-list", "false");
    	properties.setProperty("generate-structures", "false");
    	properties.setProperty("max-build-height", "256");
    	properties.setProperty("online-mode", "false");
    	properties.setProperty("level-seed", "");
    	properties.setProperty("motd",name);
    	properties.setProperty("enable-rcon","false");
    }

    public void generateFile(File gameDir) throws FileNotFoundException, IOException {
    	FileOutputStream fos = new FileOutputStream(new File(gameDir,"server.properties"));
        properties.store(fos, "Rezxis Realms");
        fos.close();
        
    }
}

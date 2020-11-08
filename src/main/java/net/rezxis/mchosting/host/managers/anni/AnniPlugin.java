package net.rezxis.mchosting.host.managers.anni;

import lombok.Getter;

public enum AnniPlugin {
	
	ANNIHILATION("https://ci.rezxis.net/job/anni/lastSuccessfulBuild/artifact/target/Annihilation-0.0.1-SNAPSHOT-jar-with-dependencies.jar"),
	ANTILOGGER("https://ci.rezxis.net/job/AntiLogger/lastSuccessfulBuild/artifact/target/AntiLogger-0.0.1-SNAPSHOT-jar-with-dependencies.jar"),
	BONUSMANAGER("https://ci.rezxis.net/job/bonusmanager/lastSuccessfulBuild/artifact/target/bonusmanager-0.0.1-SNAPSHOT.jar"),
	CHATMANAGER("https://ci.rezxis.net/job/chatmanager/lastSuccessfulBuild/artifact/target/chatmanager-0.0.1-SNAPSHOT-jar-with-dependencies.jar"),
	CORE("https://ci.rezxis.net/job/Core/lastSuccessfulBuild/artifact/target/Core-0.0.1-SNAPSHOT-jar-with-dependencies.jar"),
	MINEZSTATUS("https://ci.rezxis.net/job/MinezStatus/lastSuccessfulBuild/artifact/target/MinezStatus-0.0.1-SNAPSHOT.jar"),
	PARTIES("https://ci.rezxis.net/job/Parties/lastSuccessfulBuild/artifact/target/parties-0.0.1-SNAPSHOT.jar"),
	PREFERENCES("https://ci.rezxis.net/job/Preferences/lastSuccessfulBuild/artifact/target/Preferences-0.0.1-SNAPSHOT-jar-with-dependencies.jar"),
	RANKS("https://ci.rezxis.net/job/Ranks/lastSuccessfulBuild/artifact/target/ShotbowRanks-0.0.1-SNAPSHOT.jar"),
	SERVERSTATUSSIGN("https://ci.rezxis.net/view/Anni/job/ServerStatusSign/lastSuccessfulBuild/artifact/target/ServerStatusSigns-0.0.1-SNAPSHOT-jar-with-dependencies.jar"),
	XP("https://ci.rezxis.net/view/Anni/job/ShotBowXP/lastSuccessfulBuild/artifact/target/ShotbowXp-0.0.1-SNAPSHOT.jar"),
	SOULBOUND("https://ci.rezxis.net/view/Anni/job/SoulBound/lastSuccessfulBuild/artifact/target/SoulBound-0.0.1-SNAPSHOT.jar"),
	REZXISSQLPLUGIN("https://ci.rezxis.net/view/rezxis/job/RezxisSQLPlugin/lastSuccessfulBuild/artifact/target/RezxisSQLPlugin-0.0.1-SNAPSHOT-jar-with-dependencies.jar");
	
	@Getter private String jarName;
	@Getter private String url;
	
	AnniPlugin(String url) {
		this.url = url;
		String[] split = url.split("/");
		this.jarName = split[split.length - 1];
	}
}

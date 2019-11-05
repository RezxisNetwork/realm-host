package net.rezxis.mchosting.host;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Properties;

public class Props {
	public final String MAX_MEM;
	public final String BASE_DIR;
	public final String SERVER_JAR_NAME;
	public final boolean USE_WINDOW;
	public final int MAX_SERVERS;
	public final int HOST_ID;
	public final String SYNC_ADDRESS;
	
	final Properties prop=new Properties();
	public Props(String fname) {
        InputStream istream;
		try {
			ProtectionDomain pd = this.getClass().getProtectionDomain();
			CodeSource cs = pd.getCodeSource();
			URL location = cs.getLocation();
			URI uri = location.toURI();
			Path path = Paths.get(uri);


			istream = new FileInputStream(new File(new File(""+path).getParent(),fname));
	        prop.load(istream);
		} catch (Exception e) {
			e.printStackTrace();
		}
        MAX_MEM=prop.getProperty("max_mem");
        BASE_DIR=prop.getProperty("base_dir");
        USE_WINDOW=Boolean.parseBoolean(prop.getProperty("use_window"));
        MAX_SERVERS = Integer.valueOf(prop.getProperty("max_servers"));
        SERVER_JAR_NAME = prop.getProperty("server_jar_name");
        HOST_ID = Integer.valueOf(prop.getProperty("host_id"));
        SYNC_ADDRESS = prop.getProperty("sync_address");
	}
}

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
	public final String BASE_DIR;
	public final String SERVER_JAR_NAME;
	public final boolean USE_WINDOW;
	public final int MAX_SERVERS;
	public final boolean CPULIMIT;
	public final int HOST_ID;
	public final String SYNC_ADDRESS;
	public final int SYNC_PORT;
	public final String DB_NAME;
	public final String DB_HOST;
	public final String DB_USER;
	public final String DB_PASS;
	public final String DB_PORT;
	public final String CHILD_DB;
	public final String CHILD_SYNC;
	
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
        BASE_DIR=prop.getProperty("base_dir");
        USE_WINDOW=Boolean.parseBoolean(prop.getProperty("use_window"));
        MAX_SERVERS = Integer.valueOf(prop.getProperty("max_servers"));
        SERVER_JAR_NAME = prop.getProperty("server_jar_name");
        HOST_ID = Integer.valueOf(prop.getProperty("host_id"));
        SYNC_ADDRESS = prop.getProperty("sync_address");
        SYNC_PORT = Integer.valueOf(prop.getProperty("sync_port"));
        CHILD_DB = prop.getProperty("child_db");
        CHILD_SYNC = prop.getProperty("child_sync");
        DB_HOST=prop.getProperty("db_host");
        DB_USER=prop.getProperty("db_user");
        DB_PASS=prop.getProperty("db_pass");
        DB_PORT=prop.getProperty("db_port");
        DB_NAME=prop.getProperty("db_name");
        CPULIMIT=Boolean.parseBoolean(prop.getProperty("cpulimit"));
	}
}

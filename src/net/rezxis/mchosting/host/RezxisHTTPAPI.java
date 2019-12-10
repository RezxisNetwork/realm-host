package net.rezxis.mchosting.host;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.io.IOUtils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RezxisHTTPAPI {

	private static OkHttpClient client;
	
	static {
		client = new OkHttpClient.Builder().build();
	}
	
	public static void download(File file, String secret, String uuid) throws Exception {
		String url = "http://localhost/worlds/api.php?type=download&secretKey="+secret+"&uuid="+uuid;
		Response res = client.newCall(new Request.Builder().url(url).get().build()).execute();
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			IOUtils.copy(res.body().byteStream(), fos);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			IOUtils.closeQuietly(fos);
		}
	}
}

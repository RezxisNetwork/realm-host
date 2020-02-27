package net.rezxis.mchosting.host;

import static com.backblaze.b2.util.B2ExecutorUtils.createThreadFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;

public class BackBlazeAPI {

	private static B2StorageClient client;
	private static final B2UploadListener uploadListener;
	
	static {
		client = B2StorageClientFactory
	             .createDefaultFactory()
	             .create("52f9afdd5b98", "000298aa4fa0a5907c54eec98dadbdb6e4e622c74a", "rezxis");
		uploadListener = (progress) -> {
            final double percent = (100. * (progress.getBytesSoFar() / (double) progress.getLength()));
            System.out.println(String.format("  Upload - (%3.2f, %s)", percent, progress.toString()));
        };
	}
	
	public static void delete(String bucketName, String fileName) throws B2Exception {
		B2FileVersion file = client.getFileInfoByName(bucketName, fileName);
		client.deleteFileVersion(file);
	}
	
	public static void download(String bucketName, String fileName, File dest) throws B2Exception {
		client.downloadById(client.getFileInfoByName(bucketName, fileName).getFileId(), new B2ContentSink() {
			@Override
			public void readContent(B2Headers responseHeaders, InputStream in) throws B2Exception, IOException {
				FileUtils.copyToFile(in, dest);
				in.close();
			}});
	}
	
	public static void upload(String bucketName, String name, File target) throws B2Exception {
		B2Bucket bucket = client.getBucketOrNullByName(bucketName);
		if (target.length() >= 5000000) {
			ExecutorService executor = Executors.newFixedThreadPool(10, createThreadFactory("executor-%02d"));
			client.uploadLargeFile(B2UploadFileRequest.builder(bucket.getBucketId(), name, "", new B2ContentSource() {

				@Override
				public long getContentLength() throws IOException {
					return target.length();
				}

				@Override
				public String getSha1OrNull() throws IOException {
					return null;
				}

				@Override
				public Long getSrcLastModifiedMillisOrNull() throws IOException {
					return target.lastModified();
				}

				@Override
				public InputStream createInputStream() throws IOException {
					return new FileInputStream(target);
				}}).setListener(uploadListener).build(),executor);
		} else {
			client.uploadSmallFile(B2UploadFileRequest.builder(bucket.getBucketId(), name, "", new B2ContentSource() {

				@Override
				public long getContentLength() throws IOException {
					return target.length();
				}

				@Override
				public String getSha1OrNull() throws IOException {
					return null;
				}

				@Override
				public Long getSrcLastModifiedMillisOrNull() throws IOException {
					return target.lastModified();
				}

				@Override
				public InputStream createInputStream() throws IOException {
					return new FileInputStream(target);
				}}).setListener(uploadListener).build());
		}
	}
}

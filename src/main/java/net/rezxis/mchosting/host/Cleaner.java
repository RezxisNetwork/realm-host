package net.rezxis.mchosting.host;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import net.rezxis.mchosting.database.Database;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.internal.DBBackup;

public class Cleaner {

	public static void main(String[] args) {
		Props props = new Props("host.propertis");
		Database.init(props.DB_HOST,props.DB_USER,props.DB_PASS,props.DB_PORT,props.DB_NAME);
		File[] files = new File("backups").listFiles();
		ArrayList<DBBackup> backups = Tables.getBTable().getAll();
		ArrayList<Integer> delete = new ArrayList<>();
		for (File file : files) {
			int id = Integer.valueOf(file.getName().replace(".zip", ""));
			boolean flag = true;
			for (DBBackup backup : backups) {
				if (backup.getId() == id) {
					flag = false;
				}
			}
			if (flag)
				delete.add(id);
		}
		for (Integer i : delete) {
			try {
				FileUtils.forceDelete(new File(i + ".zip"));
				System.out.println(new File("backups/" + i + ".zip").getAbsolutePath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

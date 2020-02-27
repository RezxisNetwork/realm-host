package net.rezxis.mchosting.host;

import java.util.HashMap;
import java.util.List;

import com.github.dockerjava.api.model.Container;

import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.object.server.ServerStatus;
import net.rezxis.mchosting.host.managers.games.DockerManager;
import net.rezxis.utils.WebAPI;
import net.rezxis.utils.WebAPI.DiscordWebHookEnum;

public class HealthCheckTask implements Runnable {

	@Override
	public void run() {
		while (true) {
			HashMap<Integer,DBServer> servers = new HashMap<>();
			for (DBServer server : Tables.getSTable().getAll()) {
				if (server.getStatus() != ServerStatus.STOP && server.getStatus() != ServerStatus.BACKUP) {
					servers.put(server.getId(), server);
				}
			}
			List<Container> containers = DockerManager.getInstance().client.listContainersCmd().exec();
			for (Container container : containers) {
				if (container.getNames()[0].startsWith("container")) {
					int id = Integer.valueOf(container.getNames()[0].replace("container", ""));
					DBServer server = servers.get(id);
					if (server == null) {
						WebAPI.webhook(DiscordWebHookEnum.PRIVATE, String.format("[HealthCheck] %s container is exists but DBServer is not running", String.valueOf(id)));
						continue;
					}
					if (!DockerManager.getInstance().client.inspectContainerCmd(container.getId()).exec().getState().getRunning()) {
						WebAPI.webhook(DiscordWebHookEnum.PRIVATE, String.format("[HealthCheck] %s container is exists but it is not running", String.valueOf(id)));
						continue;
					}
				}
			}
			try {
				Thread.sleep(1000*60);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

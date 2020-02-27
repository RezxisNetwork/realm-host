package net.rezxis.mchosting.host.managers;

import net.rezxis.mchosting.database.object.server.DBServer;

public interface IGame {

	public void start(DBServer target);
	public void stopped(DBServer target);
	public void kill(DBServer target);
	public void reboot(DBServer target);
}

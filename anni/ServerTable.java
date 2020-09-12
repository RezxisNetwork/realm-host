package net.rezxis.mchosting.host.managers.anni;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import net.rezxis.mchosting.database.MySQLStorage;

public class ServerTable extends MySQLStorage {

	/*
	 *
	 varchar(20) serverName;
	 int maxPlayers;
	 int onlinePlayers;
	 boolean joinable;
	 boolean online;//?
	 varchar(40) ip:port;
	 text icon(json);
	 text line1;
	 text line2;
	 text line3;
	 text line4;
	 DATETIME lastUpdated;
	 */

	public ServerTable() {
		super("servers");;
        prefix = "rezxis_";
        tablename = "servers";
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("serverName","VARCHAR(20) PRIMARY KEY NOT NULL,");
        map.put("maxPlayers","int,");
        map.put("onlinePlayers","int,");
        map.put("joinable","boolean,");
        map.put("online","boolean,");
        map.put("ip","varchar(40),");
        map.put("icon","text,");
        map.put("line1","text,");
        map.put("line2","text,");
        map.put("line3","text,");
        map.put("line4","text,");
        map.put("lastUpdated","DATETIME");

        createTable(map);
    }

	public void check() {
		ArrayList<String> list=new ArrayList<String>();
		 executeQuery(new Query(selectFromTable("serverName,online")) {
            @Override
            protected void onResult(ResultSet resultSet) {
                try {
                	while(resultSet.next()) {
                    	String name=resultSet.getString("serverName");
                    	if(AnniManager.search(name)==null&&resultSet.getBoolean("online")) {
                    		list.add(name);
                    	}
                	}
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });

		 for(String s:list) {
			 System.out.println("delete: "+s);
     		execute("UPDATE " + getTable() + " SET maxPlayers = ?, onlinePlayers = ?,joinable = ?,online = ?,ip = ?,icon = ?,line1 = ?,line2 = ?,line3 = ?,line4 = ?,  lastUpdated = Now() WHERE serverName = ?",-1,-1,false,false,"","","","","","",s );
		 }
	}




	public void update(StatusSignInfo clan) {
        boolean row= exists(clan.getServerName());

        if(!row) {
    		execute(new Insert(insertIntoTable() + " (serverName,maxPlayers,onlinePlayers,joinable,online,ip,icon,line1,line2,line3,line4,lastUpdated) VALUES (?,?,?,?,?,?,?,?,?,?,?,Now())",
    												 clan.getServerName(),clan.getMaxPlayers(),clan.getOnlinePlayers(),clan.isJoinable(),clan.isOnline(),clan.getIp()+":"+clan.getPort(),clan.getIcon(),clan.getLine1(),clan.getLine2(),clan.getLine3(),clan.getLine4()) {
                @Override
                public void onInsert(List<Integer> integers) {
                }
            });
        }else {
    		execute("UPDATE " + getTable() + " SET maxPlayers = ?, onlinePlayers = ?,joinable = ?,online = ?,ip = ?,icon = ?,line1 = ?,line2 = ?,line3 = ?,line4 = ?,  lastUpdated = Now() WHERE serverName = ?",
    				clan.getMaxPlayers(),clan.getOnlinePlayers(),clan.isJoinable(),clan.isOnline(),clan.getIp()+":"+clan.getPort(),clan.getIcon(),clan.getLine1(),clan.getLine2(),clan.getLine3(),clan.getLine4(), clan.getServerName() );
        }

	}

	public boolean exists(String serverName) {
		return (boolean)executeQuery(new Query(selectFromTable("serverName") + " WHERE serverName = ?",serverName) {
            @Override
            protected void onResult(ResultSet resultSet) {
                try {
                	setReturnValue(resultSet.next());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
	}
}
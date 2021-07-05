package com.smallaswater.easysql.mysql.utils;

/**
 * 数据库用户信息
 *
 * @author SmallasWater
 */
public class UserData {

    private final String user;

    private final String passWorld;

    private final String host;

    private final int port;

    private final String database;


    public UserData(String user, String passWorld, String host, int port, String database) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.passWorld = passWorld;
        this.database = database;
    }

    public String getPassWorld() {
        return passWorld;
    }

    public String getDatabase() {
        return database;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }
}

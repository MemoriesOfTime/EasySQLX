package com.smallaswater.easysql.mysql.utils;


import com.smallaswater.easysql.mysql.BaseMySql;
import com.zaxxer.hikari.HikariDataSource;

/**
 * @author SmallasWater
 * Create on 2021/7/4 9:08
 * Package com.smallaswater.easysql
 */
public class LoginPool {

    public HikariDataSource dataSource;
    private final String user;

    private final String ip;

    private final String database;

    private BaseMySql manager;

    public LoginPool(String ip, String user, String database) {
        this.ip = ip;
        this.user = user;
        this.database = database;
    }

    public void setManager(BaseMySql manager) {
        this.manager = manager;
    }

    public BaseMySql getManager() {
        return manager;
    }

    @Override
    public boolean equals(Object pool) {
        if (pool instanceof LoginPool) {
            return ((LoginPool) pool).database.equalsIgnoreCase(database) &&
                    ((LoginPool) pool).user.equalsIgnoreCase(user) &&
                    ((LoginPool) pool).ip.equalsIgnoreCase(ip);
        }
        return false;
    }


}

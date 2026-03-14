package com.smallaswater.easysqlx.mysql.utils;


import com.smallaswater.easysqlx.mysql.BaseMySql;
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

    /**
     * 判断连接池是否处于活跃状态
     */
    public boolean isActive() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * 安全关闭连接池
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
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

package com.smallaswater.easysql.mysql;


import cn.nukkit.plugin.Plugin;
import com.smallaswater.easysql.EasySql;
import com.smallaswater.easysql.exceptions.MySqlLoginException;
import com.smallaswater.easysql.mysql.manager.PluginManager;
import com.smallaswater.easysql.mysql.utils.*;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库基类文件
 *
 * @author 楠木i，若水
 */
public abstract class BaseMySql extends AbstractOperation {

    private final UserData data;


    private final Plugin plugin;


    public BaseMySql(Plugin plugin, UserData data) {
        this.data = data;
        this.database = data.getDatabase();
        this.plugin = plugin;
    }

    public static String getDefaultConfig() {
        return getDefaultTable(new TableType("name", DataType.getVARCHAR())
                , new TableType("config", DataType.getTEXT()));
    }

    public static String getDefaultTable(TableType... type) {
        StringBuilder builder = new StringBuilder();
        int i = 1;
        for (TableType type1 : type) {
            builder.append(type1.toTable());
            if (i != type.length) {
                builder.append(",");
            }
            i++;
        }
        return builder.toString();

    }

    protected boolean connect() throws MySqlLoginException {
        Connection connection = null;

        try {
            this.pool = EasySql.getLoginPool(data);

            Class.forName("com.mysql.cj.jdbc.Driver");
            this.pool.dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            this.pool.dataSource.setUrl("jdbc:mysql://" + this.data.getHost() + ':' + this.data.getPort() + '/' + this.data.getDatabase() + "?&autoReconnect=true&failOverReadOnly=false&serverTimezone=GMT&characterEncoding=utf8&useSSL=false");
            this.pool.dataSource.setUsername(this.data.getUser());
            this.pool.dataSource.setPassword(this.data.getPassWorld());
            this.pool.dataSource.setInitialSize(3);
            this.pool.dataSource.setMinIdle(1);
            this.pool.dataSource.setMaxActive(30);
            this.pool.dataSource.setValidationQuery("SELECT 1");
            this.pool.dataSource.setTimeBetweenEvictionRunsMillis(180000);


//            dataSource.
            connection = this.getConnection();
            if (connection != null) {
                plugin.getLogger().info("已连接数据库");
                PluginManager.connect(plugin, this);


                return true;
            } else {
                plugin.getLogger().info(" 无法连接数据库");
            }
        } catch (ClassNotFoundException var2) {
            var2.printStackTrace();
            plugin.getLogger().info("连接数据库出现异常...");
        } finally {

            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        throw new MySqlLoginException();

    }

    public void shutdown() {
        try {
            this.pool.dataSource.getConnection().close();
            plugin.getLogger().info(" 已断开数据库连接");
        } catch (SQLException var2) {
            var2.printStackTrace();
        }

    }
}

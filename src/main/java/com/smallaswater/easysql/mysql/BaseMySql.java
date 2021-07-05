package com.smallaswater.easysql.mysql;


import cn.nukkit.plugin.Plugin;
import com.smallaswater.easysql.EasySql;
import com.smallaswater.easysql.exceptions.MySqlLoginException;
import com.smallaswater.easysql.mysql.manager.PluginManager;
import com.smallaswater.easysql.mysql.utils.AbstractOperation;
import com.smallaswater.easysql.mysql.utils.TableType;
import com.smallaswater.easysql.mysql.utils.Types;
import com.smallaswater.easysql.mysql.utils.UserData;

import java.beans.PropertyVetoException;
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
        return getDefaultTable(new TableType("name", Types.VARCHAR)
                , new TableType("config", Types.TEXT));
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

            Class.forName("com.mysql.jdbc.Driver");
            this.pool.dataSource.setDriverClass("com.mysql.jdbc.Driver");
            this.pool.dataSource.setDebugUnreturnedConnectionStackTraces(false);

            this.pool.dataSource.setJdbcUrl("jdbc:mysql://" + this.data.getHost() + ':' + this.data.getPort() + '/' + this.database + "?&autoReconnect=true&failOverReadOnly=false&serverTimezone=GMT&characterEncoding=utf8&useSSL=false");
            this.pool.dataSource.setUser(this.data.getUser());
            this.pool.dataSource.setPassword(this.data.getPassWorld());
            this.pool.dataSource.setInitialPoolSize(3);
            this.pool.dataSource.setAcquireIncrement(1);
            this.pool.dataSource.setMinPoolSize(1);
            this.pool.dataSource.setMaxPoolSize(30);
            this.pool.dataSource.setMaxStatements(50);
            this.pool.dataSource.setAutoCommitOnClose(true);
            this.pool.dataSource.setTestConnectionOnCheckout(true);
            this.pool.dataSource.setMaxIdleTime(1800);
            this.pool.dataSource.setPreferredTestQuery("SELECT 1");
            this.pool.dataSource.setMaxIdleTime(30);
            this.pool.dataSource.setIdleConnectionTestPeriod(1800);


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
        } catch (PropertyVetoException e) {
            e.printStackTrace();
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

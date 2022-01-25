package com.smallaswater.easysql.orm.api;

import cn.nukkit.plugin.Plugin;
import com.smallaswater.easysql.EasySql;
import com.smallaswater.easysql.exceptions.MySqlLoginException;
import com.smallaswater.easysql.mysql.utils.LoginPool;
import com.smallaswater.easysql.mysql.utils.UserData;
import com.smallaswater.easysql.orm.api.example.ExampleDAO;
import com.smallaswater.easysql.orm.handle.ORMDynaProxyHandle;
import com.smallaswater.easysql.orm.handle.ORMStdHandle;
import com.smallaswater.easysql.v3.mysql.manager.SqlManager;
import lombok.Setter;

public class ORMHandleBuilder {

    @Setter
    private Plugin plugin;

    private String user;

    private String passWorld;

    private String host;

    private int port;

    private String database;

    @Setter
    private String table;

    private UserData userData;

    private SqlManager manager;

    private LoginPool pool;

    public static ORMHandleBuilder builder(Plugin plugin) {
        ORMHandleBuilder builder = new ORMHandleBuilder();
        builder.setPlugin(plugin);
        return builder;
    }

    public static ORMHandleBuilder builder(Plugin plugin, String table) {
        ORMHandleBuilder builder = new ORMHandleBuilder();
        builder.setPlugin(plugin);
        builder.setTable(table);
        return builder;
    }

    public ORMHandleBuilder fromUserData(UserData userData) {
        this.userData = userData;
        this.user = userData.getUser();
        this.passWorld = userData.getPassWorld();
        this.host = userData.getHost();
        this.port = userData.getPort();
        this.database = userData.getDatabase();
        return this;
    }

    public ORMHandleBuilder table(String table) {
        this.table = table;
        return this;
    }

    public ORMHandleBuilder buildHost() {
        if (this.userData == null) {
            this.userData = new UserData(this.user, this.passWorld, this.host, this.port, this.database);
        }
        try {
            this.manager = new SqlManager(this.plugin, this.userData);
        } catch (MySqlLoginException e) {
            e.printStackTrace();
        }
        this.pool = EasySql.getLoginPool(this.userData);
        return this;
    }


    public ORMStdHandle buildStdHandle() {
        // TODO
        return new ORMStdHandle();
    }

    public <T> IDAO<T> buildORMDynaProxyIDAO(Class<? extends IDAO<T>> dao) {
        return new ORMDynaProxyHandle<>(dao, this.table, this.manager).getProxyInstance();
    }

    public <T> IDAO<T> buildORMDynaProxyIDAO(String tableName, Class<? extends IDAO<T>> clazz) {
        return new ORMDynaProxyHandle<>(clazz, tableName, this.manager).getProxyInstance();
    }

}

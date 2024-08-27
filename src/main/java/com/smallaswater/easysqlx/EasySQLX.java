package com.smallaswater.easysqlx;


import cn.nukkit.plugin.PluginBase;
import com.smallaswater.easysqlx.mysql.utils.LoginPool;
import com.smallaswater.easysqlx.mysql.utils.UserData;
import com.smallaswater.easysqlx.sqlite.SQLiteHelper;
import lombok.Getter;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @author SmallasWater
 */
public class EasySQLX extends PluginBase {

    @Getter
    private static EasySQLX instance;

    private static final ArrayList<LoginPool> pools = new ArrayList<>();

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.getLogger().info("已加载 EasySQL 插件 v" + this.getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        // 无法保证在所有插件卸载后再关闭数据库连接
        /*for (BaseMySql mysql : PluginManager.getList()) {
            if (mysql != null) {
                mysql.shutdown();
            }
        }*/
    }

    public static LoginPool getLoginPool(UserData data) {
        LoginPool pool = new LoginPool(data.getHost(), data.getUser(), data.getDatabase());
        if(!pools.contains(pool)){
            pools.add(pool);
        }
        return pools.get(pools.indexOf(pool));
    }

    public static SQLiteHelper getSQLiteHelper(String path) throws SQLException, ClassNotFoundException {
        return new SQLiteHelper(path);
    }
}

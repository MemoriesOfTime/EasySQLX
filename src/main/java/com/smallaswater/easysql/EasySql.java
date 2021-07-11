package com.smallaswater.easysql;


import cn.nukkit.plugin.PluginBase;
import com.smallaswater.easysql.mysql.BaseMySql;
import com.smallaswater.easysql.mysql.manager.PluginManager;
import com.smallaswater.easysql.mysql.utils.LoginPool;
import com.smallaswater.easysql.mysql.utils.UserData;

import java.util.ArrayList;


/**
 * @author SmallasWater
 */
public class EasySql extends PluginBase {

    private static final ArrayList<LoginPool> pools = new ArrayList<>();

    @Override
    public void onEnable() {
        this.getLogger().info("已加载 EasyMySQL 插件 v"+this.getDescription().getVersion());
    }

    public static LoginPool getLoginPool(UserData data) {
        LoginPool pool = new LoginPool(data.getHost(), data.getUser(), data.getDatabase());
        if(!pools.contains(pool)){
            pools.add(pool);
        }
        return pools.get(pools.indexOf(pool));
    }


    @Override
    public void onDisable() {
        for (BaseMySql mysql : PluginManager.getList()) {
            if (mysql != null) {
                mysql.shutdown();
            }
        }
    }
}

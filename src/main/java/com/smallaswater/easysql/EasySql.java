package com.smallaswater.easysql;


import cn.nukkit.plugin.PluginBase;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.smallaswater.easysql.mysql.BaseMySql;
import com.smallaswater.easysql.mysql.manager.PluginManager;
import com.smallaswater.easysql.mysql.utils.LoginPool;
import com.smallaswater.easysql.mysql.utils.UserData;

import java.util.ArrayList;


/**
 * @author SmallasWater
 */
public class EasySql extends PluginBase {

    private static ArrayList<LoginPool> pools = new ArrayList<>();

    @Override
    public void onEnable() {

        this.getLogger().info("已加载 EasyMySQL 插件 v2.0.3");
    }

    public static LoginPool getLoginPool(UserData data){
        LoginPool pool = new LoginPool(data.getHost(),data.getUser(),data.getDatabase());
        if(!pools.contains(pool)){
            pools.add(pool);
        }
        return pools.get(pools.indexOf(pool));
    }


    @Override
    public void onDisable() {
        for (BaseMySql mysql : PluginManager.getList()) {
            if(mysql != null) {
                mysql.shutdown();
            }
        }
    }
}

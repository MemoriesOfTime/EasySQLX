package com.smallaswater.easysql.mysql.manager;


import cn.nukkit.plugin.Plugin;
import com.smallaswater.easysql.mysql.BaseMySql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 缓存 数据库对象
 *
 * @author 若水
 */
public class PluginManager {
    private static final HashMap<Plugin, BaseMySql> CONNECTION = new HashMap<>();


    public PluginManager() {
    }

    public static BaseMySql getData(Plugin plugin) {
        return CONNECTION.get(plugin);
    }

    public static List<BaseMySql> getList() {
        return new ArrayList<>(CONNECTION.values());
    }

    public static void connect(Plugin plugin, BaseMySql baseMySql) {
        CONNECTION.put(plugin, baseMySql);
    }
}

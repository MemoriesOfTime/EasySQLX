package com.smallaswater.easysql.mysql.manager;


import cn.nukkit.plugin.Plugin;
import com.smallaswater.easysql.exceptions.MySqlLoginException;
import com.smallaswater.easysql.mysql.BaseMySql;
import com.smallaswater.easysql.mysql.utils.TableType;
import com.smallaswater.easysql.mysql.utils.UserData;


/**
 * @author SmallasWater
 */
public class SqlManager extends BaseMySql {

    private boolean isEnable = false;

    public SqlManager(Plugin plugin, String configTableName, UserData data) throws MySqlLoginException {
        super(plugin, data);
        if (connect()) {
            this.form = configTableName;
        }

    }


    public SqlManager(Plugin plugin, String configTableName, UserData data, TableType... table) throws MySqlLoginException {
        super(plugin, data);
        if (connect()) {
            isEnable = true;
            this.form = configTableName;
            if (table.length > 0) {
                if (createTable(getConnection(), configTableName, BaseMySql.getDefaultTable(table))) {
                    plugin.getLogger().info("创建数据表" + configTableName + "成功");
                }
            }
        }


    }

    /**
     * 配置专用
     *
     * @param plugin 插件
     * @param data 用户配置
     * @param configTableName 数据库表名
     */
    public SqlManager(Plugin plugin, UserData data, String configTableName) throws MySqlLoginException {
        super(plugin, data);
        if (connect()) {
            this.form = configTableName;
            if (createTable(getConnection(), configTableName, BaseMySql.getDefaultConfig())) {
                plugin.getLogger().info("创建数据表成功");
            }
        }


    }

    public boolean isEnable() {
        return isEnable;
    }

    public void disable() {
        PluginManager.getList().remove(this);
        this.shutdown();
    }


}

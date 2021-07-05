package com.smallaswater.easysql.mysql.manager;

import cn.nukkit.plugin.Plugin;
import com.smallaswater.easysql.exceptions.MySqlLoginException;
import com.smallaswater.easysql.mysql.BaseMySql;
import com.smallaswater.easysql.mysql.utils.UserData;

/**
 * 根据表名使用数据库
 *
 * @author SmallasWater
 */
public class UseTableSqlManager extends BaseMySql {

    public UseTableSqlManager(Plugin plugin, UserData data, String tableName) throws MySqlLoginException {
        super(plugin, data);
        connect();
        this.form = tableName;
    }
}

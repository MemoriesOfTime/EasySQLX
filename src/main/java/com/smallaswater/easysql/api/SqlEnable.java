package com.smallaswater.easysql.api;


import cn.nukkit.plugin.Plugin;
import com.smallaswater.easysql.exceptions.MySqlLoginException;
import com.smallaswater.easysql.mysql.manager.SqlManager;
import com.smallaswater.easysql.mysql.manager.UseTableSqlManager;
import com.smallaswater.easysql.mysql.utils.TableType;
import com.smallaswater.easysql.mysql.utils.UserData;
import org.jetbrains.annotations.NotNull;


/**
 * <p>
 * mysql工具类 使用的时候 实例化本类
 *
 * </p>
 *
 * @author 楠木i, 若水
 */
@Deprecated
public class SqlEnable {

    private final SqlManager manager;
    private final UserData data;

    public SqlEnable(@NotNull Plugin plugin, @NotNull UserData data) throws MySqlLoginException {
        this.data = data;
        this.manager = new SqlManager(plugin, data);
    }

    @Deprecated
    public SqlEnable(@NotNull Plugin plugin, String tableName, UserData data, TableType... tables) throws MySqlLoginException {
        this.data = data;
        this.manager = new UseTableSqlManager(plugin, data, tableName);
        if (tableName != null && !"".equals(tableName.trim()) && tables.length > 0) {
            if (this.manager.createTable(tableName, tables)) {
                plugin.getLogger().info("创建数据表" + tableName + "成功");
            }
        }
    }

    public boolean isEnable() {
        return this.manager.isEnable();
    }

    /**
     * 关闭服务器的时候记得执行这个
     */
    public void disable() {
        this.manager.disable();
    }

    public UserData getData() {
        return this.data;
    }

    public SqlManager getManager() {
        return this.manager;
    }
}

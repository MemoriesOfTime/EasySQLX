package com.smallaswater.easysql.mysql.manager;

import cn.nukkit.plugin.Plugin;
import com.smallaswater.easysql.exceptions.MySqlLoginException;
import com.smallaswater.easysql.mysql.utils.TableType;
import com.smallaswater.easysql.mysql.utils.UserData;
import lombok.Getter;

/**
 * 根据表名使用数据库
 *
 * @author SmallasWater
 */
public class UseTableSqlManager extends SqlManager {

    @Getter
    protected String tableName;

    public UseTableSqlManager(Plugin plugin, UserData data, String tableName) throws MySqlLoginException {
        super(plugin, data);
        this.tableName = tableName;
    }

    /**
     * 给表增加字段
     *
     * @param tableType 字段参数
     * @return 增加一个字段
     */
    public boolean createColumn(TableType tableType) {
        return super.createColumn(this.tableName, tableType);
    }

    @Deprecated //对于UseTableSqlManager来说不推荐使用此方法
    @Override
    public boolean createColumn(String tableName, TableType tableType) {
        return super.createColumn(tableName, tableType);
    }

    public void deleteTable() {
        super.deleteTable(this.tableName);
    }

    @Deprecated //对于UseTableSqlManager来说不推荐使用此方法
    @Override
    public void deleteTable(String tableName) {
        super.deleteTable(tableName);
    }

    public boolean isExistColumn(String column) {
        return super.isExistColumn(this.tableName, column);
    }

    @Deprecated //对于UseTableSqlManager来说不推荐使用此方法
    @Override
    public boolean isExistColumn(String table, String column) {
        return super.isExistColumn(table, column);
    }

    /**
     * 给表删除字段
     *
     * @param args 字段名
     * @return 删除一个字段
     */
    public boolean deleteColumn(String args) {
        return super.deleteColumn(args, this.tableName);
    }

    @Deprecated //对于UseTableSqlManager来说不推荐使用此方法
    @Override
    public boolean deleteColumn(String args, String tableName) {
        return super.deleteColumn(args, tableName);
    }
}

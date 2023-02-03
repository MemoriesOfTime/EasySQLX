package com.smallaswater.easysql.v3.mysql.manager;

import cn.nukkit.plugin.Plugin;
import com.smallaswater.easysql.exceptions.MySqlLoginException;
import com.smallaswater.easysql.mysql.data.SqlData;
import com.smallaswater.easysql.mysql.data.SqlDataList;
import com.smallaswater.easysql.mysql.utils.ChunkSqlType;
import com.smallaswater.easysql.mysql.utils.TableType;
import com.smallaswater.easysql.mysql.utils.UserData;
import com.smallaswater.easysql.v3.mysql.utils.SelectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

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
    public boolean createColumn(@NotNull String tableName, @NotNull TableType tableType) {
        return super.createColumn(tableName, tableType);
    }

    public void deleteTable() {
        super.deleteTable(this.tableName);
    }

    @Deprecated //对于UseTableSqlManager来说不推荐使用此方法
    @Override
    public void deleteTable(@NotNull String tableName) {
        super.deleteTable(tableName);
    }

    public boolean isExistColumn(String column) {
        return super.isExistColumn(this.tableName, column);
    }

    @Deprecated //对于UseTableSqlManager来说不推荐使用此方法
    @Override
    public boolean isExistColumn(@NotNull String tableName, @NotNull String column) {
        return super.isExistColumn(tableName, column);
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
    public boolean deleteColumn(@NotNull String args, String tableName) {
        return super.deleteColumn(args, tableName);
    }

    /**
     * 是否有数据
     *
     * @param column 条件:字段
     * @param data 条件:值
     * @return 是否存在数据
     */
    public boolean isExistsData(String column, String data) {
        return this.isExistsData(this.tableName, column, data);
    }

    /**
     * 修改数据
     *
     * @param data  数据
     * @param where 参数判断
     * @return 是否修改成功
     */
    public boolean setData(SqlData data, SqlData where) {
        return this.setData(this.tableName, data, where);
    }

    /**
     * 添加数据
     *
     * @param data 数据
     * @return 是否添加成功
     */
    public boolean insertData(SqlData data) {
        return this.insertData(this.tableName, data);
    }

    /**
     * 添加多条数据
     *
     * @param datas     数据列表
     * @return 是否添加成功
     */
    public boolean insertData(LinkedList<SqlData> datas) {
        return this.insertData(this.tableName, datas);
    }

    /**
     * 删除数据
     *
     * @param data 数据
     * @return 是否删除成功
     */
    public boolean deleteData(SqlData data) {
        return this.deleteData(this.tableName, data);
    }

    /**
     * 获取数据条数
     */
    public int getDataSize(String sql, ChunkSqlType... sqlType) {
        return this.getDataSize(sql, this.tableName, sqlType);
    }

    /**
     * 获取数据
     *
     * @param selectType 查询条件
     * @return 数据
     */
    public SqlDataList<SqlData> getData(SelectType selectType) {
        return this.getData(this.tableName, selectType);
    }

}

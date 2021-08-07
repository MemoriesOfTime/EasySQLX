package com.smallaswater.easysql.mysql.utils;


import com.smallaswater.easysql.mysql.data.SqlDataManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * 这里是实现类
 *
 * @author 若水
 */
public abstract class AbstractOperation {

    protected String database;

    protected LoginPool pool;

    protected String form;

//    protected Connection connection;

    private PreparedStatement preparedStatement;

    /**
     * 给表增加字段
     *
     * @param types 字段参数
     * @param args  字段名
     * @return 增加一个字段
     */

    public boolean createColumn(Types types, String args) {
        return createColumn(types, form, args);
    }


    /**
     * 给表增加字段
     *
     * @param form  表单名
     * @param types 字段参数
     * @param args  字段名
     * @return 增加一个字段
     */

    public boolean createColumn(Types types, String form, String args) {

        String command = "ALTER TABLE " + form + " ADD " + args + " " + types.toString();
        return getSqlManager().runSql(command);
    }

    public String getForm() {
        return form;
    }

    /**
     * 给表删除字段
     *
     * @param args 字段名
     * @return 删除一个字段
     */

    public boolean deleteColumn(String args) {
        String command = "ALTER TABLE " + form + " DROP ?";
        return getSqlManager().runSql(command, new ChunkSqlType(1, args));
    }

    /**
     * 给表删除字段
     *
     * @param args 字段名
     * @param form 表单名称
     * @return 删除一个字段
     */

    public boolean deleteColumn(String args, String form) {
        String command = "ALTER TABLE " + form + " DROP ?";
        return getSqlManager().runSql(command, new ChunkSqlType(1, args));
    }

    public Connection getConnection() {
        try {
            return pool.dataSource.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 获取 manager
     */
    public SqlDataManager getSqlManager() {
        return new SqlDataManager(database, form, this);
    }


    /**
     * 删除表
     */
    public void deleteField(String tableName) {
        String sql = "DROP TABLE " + tableName;
        this.getSqlManager().runSql(sql);
    }

    /**
     * 获取数据条数
     */
    public int getDataSize(String sql) {
        int i = 0;
        ResultSet resultSet = null;
        Connection collection = getConnection();
        try {
            this.preparedStatement = collection.prepareStatement("SELECT COUNT(*) FROM " + form + " " + sql);
            resultSet = this.preparedStatement.executeQuery();
            if (resultSet != null) {
                if (resultSet.next()) {
                    i = resultSet.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //                collection.close();
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            try {
                this.preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return i;
    }


    /**
     * 创建表单 SQL指令 每个args代表指令中的 ?
     *
     * @param connection 数据库连接
     * @param args       参数
     * @return 是否创建成功
     */
    public boolean createTable(Connection connection, String... args) {
        String command = "CREATE TABLE " + args[0] + "(?)engine=InnoDB default charset=utf8";
        try {
            ResultSet resultSet = connection.getMetaData().getTables(null, null, args[0], null);
            if (!resultSet.next()) {
                return getSqlManager().runSql(command, new ChunkSqlType(1, args[1]));
            }
        } catch (SQLException var5) {
            var5.printStackTrace();
        }

        return false;
    }


    /**
     * 是否存在字段
     *
     * @param table  表名
     * @param column 字段名
     * @return 是否存在
     */
    public boolean isColumn(String table, String column) {
        try {
            ResultSet resultSet = this.getConnection().getMetaData().getColumns(null, null, table, column);
            return resultSet.next();
        } catch (SQLException var4) {
            return false;
        }
    }

}

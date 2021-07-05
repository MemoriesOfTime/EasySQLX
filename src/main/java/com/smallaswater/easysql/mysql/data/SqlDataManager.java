package com.smallaswater.easysql.mysql.data;


import com.smallaswater.easysql.mysql.utils.AbstractOperation;
import com.smallaswater.easysql.mysql.utils.ChunkSqlType;
import com.smallaswater.easysql.mysql.utils.MySqlFunctions;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * 这里是一个manager
 *
 * @author SmallasWater
 */
public class SqlDataManager {


    private static final LinkedBlockingQueue<Runnable> QUEUE = new LinkedBlockingQueue<>(5);

    private static final ThreadPoolExecutor THREAD_POOL = new ThreadPoolExecutor(5, 20, 60, TimeUnit.SECONDS, QUEUE, new ThreadPoolExecutor.AbortPolicy());

    private final AbstractOperation manager;

    private final String database;

    private String tableName;

    private PreparedStatement preparedStatement;

    public SqlDataManager(String database, String tableName, AbstractOperation manager) {
        this.tableName = tableName;
        this.manager = manager;
        this.database = database;
    }

    /**
     * 设置当前表
     * @param tableName 表名
     * */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * 获取当前表
     *
     * @return 表名
     */
    public String getTableName() {
        return tableName;
    }


    /**
     * 限制条数的分组查询
     *
     * @param column  字段名
     * @param form    表名 null 或不写为当前表
     * @param where   条件
     * @param like    相似条件
     * @param start   开始位置
     * @param length  条数
     * @param groupBy 分组条件
     * @param orderBy 排序条件
     * @param having  分组的判断条件
     * @param types   防SQL注入传参 1为第一个问号
     *
     * @return 数据列表
     */
    public SqlDataList<SqlData> selectExecute(String column, String form, String where, String like, int start, int length, String groupBy, String orderBy, String having, ChunkSqlType... types) {
        String tableName = this.tableName;

        if (form != null && !"".equalsIgnoreCase(form)) {
            tableName = form;
        }
        String sql = "SELECT " + column + " FROM " + tableName;
        if (where != null && !"".equalsIgnoreCase(where)) {
            sql = sql + " WHERE " + where;
        }
        if (like != null && !"".equalsIgnoreCase(like)) {
            sql = sql + "LIKE " + like;
        }
        if (groupBy != null && !"".equalsIgnoreCase(groupBy)) {
            sql = sql + " GROUP BY " + groupBy;
        }
        if (having != null && !"".equalsIgnoreCase(having)) {
            sql = sql + " HAVING " + having;
        }
        if (start != 0 && length != 0) {
            sql = sql + " LIMIT " + start + "," + length;
        }
        if (orderBy != null && !"".equalsIgnoreCase(orderBy)) {
            sql = sql + " ORDER BY " + orderBy;
        }


        return selectExecute(sql, types);
    }


    /**
     * 根据限制查询
     *
     * @param column 字段名
     * @param form   表名 null 或不写为当前表
     * @param where  条件
     * @param types  防SQL注入传参 1为第一个问号
     */
    public SqlDataList<SqlData> selectExecute(String column, String form, String where, ChunkSqlType... types) {
        return selectExecute(column, form, where, null, 0, 0, null, null, null, types);
    }

    /**
     * 根据分组限制查询
     *
     * @param column  字段名
     * @param form    表名 null 或不写为当前表
     * @param groupBy 分组条件
     * @param having  分组后的条件判断
     * @param types   防SQL注入传参 1为第一个问号
     */
    public SqlDataList<SqlData> selectExecute(String column, String form, String groupBy, String having, ChunkSqlType... types) {
        return selectExecute(column, form, null, null, 0, 0, groupBy, null, having, types);
    }

    /**
     * 限制条数查询
     *
     * @param column 字段名
     * @param form   表名 null 或不写为当前表
     * @param where  分组条件
     * @param length 查询条数
     * @param types  防SQL注入传参 1为第一个问号
     */
    public SqlDataList<SqlData> selectExecute(String column, String form, String where, int length, ChunkSqlType... types) {
        return selectExecute(column, form, where, null, 0, length, null, null, null, types);
    }

    /**
     * 分页查询
     *
     * @param column 字段名
     * @param form   表名 null 或不写为当前表
     * @param where  分组条件
     * @param start  开始位置
     * @param length 查询条数
     * @param types  防SQL注入传参 1为第一个问号
     */
    public SqlDataList<SqlData> selectExecute(String column, String form, String where, int start, int length, ChunkSqlType... types) {
        return selectExecute(column, form, where, null, start, length, null, null, null, types);
    }


    /**
     * 执行查询SQL指令
     *
     * @param types    防SQL注入参数
     * @param commands sql语句
     */
    public SqlDataList<SqlData> selectExecute(String commands, ChunkSqlType... types) {
        SqlDataList<SqlData> objects = new SqlDataList<>(commands, types);
        Connection connection = manager.getConnection();
        try {
            this.preparedStatement = connection.prepareStatement(commands);
            if (types.length > 0) {
                for (ChunkSqlType types1 : types) {
                    this.preparedStatement.setString(types1.getI(), types1.getValue());
                }
            }
            ResultSet resultSet = this.preparedStatement.executeQuery();
            if (resultSet != null) {
                ResultSetMetaData data;
                resultSet = this.preparedStatement.executeQuery();
                while (resultSet.next()) {
                    data = this.preparedStatement.getMetaData();
                    int columnCount = data.getColumnCount();
                    SqlData map = new SqlData();
                    for (int i = 0; i < columnCount; i++) {
                        map.put(data.getColumnName(i + 1).toLowerCase(), resultSet.getObject(i + 1));
                    }
                    objects.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return objects;

        } finally {
            try {
//                connection.close();
                if (this.preparedStatement != null) {
                    this.preparedStatement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return objects;
    }


    /**
     * 判断数据是否存在
     *
     * @param column 字段名
     * @param data   数据
     * @return 是否存在
     */
    public boolean isExists(String column, String data) {
        String sql = "SELECT " + MySqlFunctions.getFunction(MySqlFunctions.SqlFunctions.COUNT, "*") + " c FROM " + tableName + " WHERE " + column + " = ?";
        return selectExecute(sql, new ChunkSqlType(1, data)).get().getInt("c") > 0;
    }

    /**
     * 判断数据是否存在
     *
     * @param column    字段名
     * @param data      数据
     * @param tableName 表单名称
     * @return 是否存在
     */
    public boolean isExists(String column, String data, String tableName) {
        String sql = "SELECT " + MySqlFunctions.getFunction(MySqlFunctions.SqlFunctions.COUNT, "*") + " c FROM " + tableName + " WHERE " + column + " = ?";
        return selectExecute(sql, new ChunkSqlType(1, data)).get().getInt("c") > 0;
    }

    /**
     * 删除数据
     *
     * @param data 数据
     * @return 是否删除成功
     */
    public boolean deleteData(SqlData data) {
        return deleteData(data, tableName);
    }

    /**
     * 删除数据
     *
     * @param data      数据
     * @param tableName 表单名称
     * @return 是否删除成功
     */
    public boolean deleteData(SqlData data, String tableName) {
        StringBuilder cmd = new StringBuilder("DELETE FROM " + tableName + " WHERE ");
        ArrayList<ChunkSqlType> objects = new ArrayList<>();
        int i = 0;
        for (java.lang.String column : data.getData().keySet()) {
            if (i >= 1) {
                cmd.append(" and ").append(column).append(" = ?");
            } else {
                cmd.append(column).append(" = ?");
            }
            objects.add(new ChunkSqlType(i + 1, data.getData().get(column).toString()));
            i++;
        }

        return runSql(cmd.toString(), objects.toArray(new ChunkSqlType[]{}));
    }


    /**
     * 添加多条数据
     *
     * @param datas 数据列表
     * @return 是否添加成功
     */
    public boolean insertData(LinkedList<SqlData> datas) {
        return insertData(datas, tableName);
    }

    /**
     * 添加多条数据
     *
     * @param datas     数据列表
     * @param tableName 表单名称
     * @return 是否添加成功
     */
    public boolean insertData(LinkedList<SqlData> datas, String tableName) {
        for (SqlData data : datas) {
            THREAD_POOL.execute(() -> insertData(data, tableName));

        }
        return true;
    }

    /**
     * 执行SQL语句
     *
     * @param sql   SQL 语句
     * @param value 防SQL注入
     * @return 是否执行成功
     */
    public boolean runSql(String sql, ChunkSqlType... value) {
        Connection connection = manager.getConnection();
        try {
            this.preparedStatement = connection.prepareStatement(sql);
            for (ChunkSqlType type : value) {
                this.preparedStatement.setString(type.getI(), type.getValue());
            }
            this.preparedStatement.execute();
            this.preparedStatement.close();
            return true;

        } catch (SQLException e) {
            System.out.println("执行 " + sql + " 语句出现异常");
            System.out.println(e.getMessage());
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        return false;
    }


    private String getUpDataWhere(SqlData data) {
        StringBuilder builder = new StringBuilder();
        for (String column : data.getData().keySet()) {
            builder.append(column).append(" = ? and");
        }
        String str = builder.toString();
        return str.substring(0, str.length() - 3);
    }

    private String getUpDataColumn(SqlData data) {
        StringBuilder builder = new StringBuilder();
        for (String column : data.getData().keySet()) {
            builder.append(column).append(" = ?,");
        }
        String str = builder.toString();
        return str.substring(0, str.length() - 1);
    }

    /**
     * 判断表是否存在
     *
     * @param tableName 表名
     */
    public boolean isTableColumnData(String tableName) {
        return isTableColumnData(tableName, null);
    }

    /**
     * 判断表是否存在
     *
     * @param tableName 表名
     * @param database  数据库名
     */
    public boolean isTableColumnData(String tableName, String database) {
        String data = this.database;
        if (database != null && !"".equalsIgnoreCase(database)) {
            data = database;
        }
        String command = "SELECT * FROM information_schema.TABLES  WHERE table_schema =? AND table_name = ?";
        return selectExecute(command, new ChunkSqlType(1, data), new ChunkSqlType(2, tableName)).size() == 0;
    }


    /**
     * 修改数据
     *
     * @param data  数据
     * @param where 参数判断
     */
    public boolean setData(SqlData data, SqlData where, String tableName) {
        ArrayList<ChunkSqlType> objects = new ArrayList<>();
        int i = 1;
        for (Map.Entry<String, Object> data1 : data.getData().entrySet()) {
            objects.add(new ChunkSqlType(i, data1.getValue().toString()));
            i++;
        }
        for (Map.Entry<String, Object> data1 : where.getData().entrySet()) {
            objects.add(new ChunkSqlType(i, data1.getValue().toString()));
            i++;
        }

        String sql = "UPDATE " + tableName + " SET " + getUpDataColumn(data) + " WHERE " + getUpDataWhere(where);
        return runSql(sql, objects.toArray(new ChunkSqlType[]{}));
    }


    /**
     * 修改数据
     *
     * @param data  数据
     * @param where 参数判断
     */
    public boolean setData(SqlData data, SqlData where) {
        return setData(data, where, tableName);
    }

    /**
     * 添加数据
     *
     * @param data 数据
     */
    public boolean insertData(SqlData data) {
        return insertData(data, tableName);
    }

    /**
     * 添加数据
     *
     * @param data 数据
     */
    public boolean insertData(SqlData data, String tableName) {
        String column = data.getColumnToString();
        String values = data.getObjectToString();
        String sql = "INSERT INTO " + tableName + " (" + column + ") VALUES (?)";
        return runSql(sql, new ChunkSqlType(1, values));
    }


    /**
     * 单执行MySQL函数
     * 请执行 danhanghans
     *
     * @param functions 封装的函数
     * @return 返回值
     */
    public SqlData executeFunction(MySqlFunctions functions) {
        return selectExecute(functions.getCommand()).get();
    }

    /**
     * 单执行MySQL函数
     * 请执行 danhanghans
     *
     * @param functions 自定义的函数 例如 COUNT(*)
     * @return 返回值
     */
    public SqlData executeFunction(String functions) {
        return selectExecute(functions).get();
    }
}

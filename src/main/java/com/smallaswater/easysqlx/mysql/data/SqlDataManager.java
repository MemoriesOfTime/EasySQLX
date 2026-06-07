package com.smallaswater.easysqlx.mysql.data;


import cn.nukkit.Server;
import com.smallaswater.easysqlx.common.data.SqlData;
import com.smallaswater.easysqlx.common.data.SqlDataList;
import com.smallaswater.easysqlx.mysql.utils.ChunkSqlType;
import com.smallaswater.easysqlx.mysql.utils.LoginPool;
import com.smallaswater.easysqlx.mysql.utils.MySqlFunctions;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * 数据库数据操作类
 *
 * @author SmallasWater
 */
public class SqlDataManager {

    private static final int BATCH_SIZE = 500;

    private SqlDataManager() {
        throw new RuntimeException();
    }

    /**
     * 限制条数的分组查询
     *
     * @param column  字段名
     * @param tableName    表名
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
    public static SqlDataList<SqlData> selectExecute(@NotNull LoginPool loginPool,
                                                     @NotNull String column,
                                                     @NotNull String tableName,
                                                     String where, String like,
                                                     int start,
                                                     int length,
                                                     String groupBy,
                                                     String orderBy,
                                                     String having,
                                                     ChunkSqlType... types) {
        Objects.requireNonNull(loginPool);
        if ("".equalsIgnoreCase(column.trim())) {
            throw new NullPointerException();
        }
        if ("".equalsIgnoreCase(tableName.trim())) {
            throw new NullPointerException();
        }
        boolean hasWhere = where != null && !"".equalsIgnoreCase(where.trim());
        String sql = "SELECT " + column + " FROM " + tableName;
        if (hasWhere) {
            sql = sql + " WHERE " + where;
        }
        if (like != null && !"".equalsIgnoreCase(like.trim())) {
            sql = sql + (hasWhere ? " LIKE " : " WHERE ") + like;
        }
        if (groupBy != null && !"".equalsIgnoreCase(groupBy.trim())) {
            sql = sql + " GROUP BY " + groupBy;
        }
        if (having != null && !"".equalsIgnoreCase(having.trim())) {
            sql = sql + " HAVING " + having;
        }
        if (orderBy != null && !"".equalsIgnoreCase(orderBy.trim())) {
            sql = sql + " ORDER BY " + orderBy;
        }
        if (length != 0) {
            sql = sql + " LIMIT " + start + "," + length;
        }

        return selectExecute(loginPool, sql, types);
    }

    /**
     * 根据限制查询
     *
     * @param column 字段名
     * @param form   表名 null 或不写为当前表
     * @param where  条件
     * @param types  防SQL注入传参 1为第一个问号
     */
    public static SqlDataList<SqlData> selectExecute(LoginPool loginPool,String column, String form, String where, ChunkSqlType... types) {
        return selectExecute(loginPool, column, form, where, null, 0, 0, null, null, null, types);
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
    public static SqlDataList<SqlData> selectExecute(LoginPool loginPool, String column, String form, String groupBy, String having, ChunkSqlType... types) {
        return selectExecute(loginPool, column, form, null, null, 0, 0, groupBy, null, having, types);
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
    public static SqlDataList<SqlData> selectExecute(LoginPool loginPool, String column, String form, String where, int length, ChunkSqlType... types) {
        return selectExecute(loginPool, column, form, where, null, 0, length, null, null, null, types);
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
    public static SqlDataList<SqlData> selectExecute(LoginPool loginPool, String column, String form, String where, int start, int length, ChunkSqlType... types) {
        return selectExecute(loginPool, column, form, where, null, start, length, null, null, null, types);
    }

    /**
     * 执行查询SQL指令
     *
     * @param types    防SQL注入参数
     * @param commands @Language("SQL")
     *
     */
    public static SqlDataList<SqlData> selectExecute(LoginPool loginPool, String commands, ChunkSqlType... types) {
        SqlDataList<SqlData> objects = new SqlDataList<>(commands, types);
        try (Connection connection = loginPool.dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(commands)) {
            bindParameters(preparedStatement, types);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                ResultSetMetaData data = resultSet.getMetaData();
                int columnCount = data.getColumnCount();
                String[] columnNames = new String[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    columnNames[i - 1] = data.getColumnName(i).toLowerCase();
                }
                while (resultSet.next()) {
                    SqlData map = new SqlData();
                    for (int i = 1; i <= columnCount; i++) {
                        map.put(columnNames[i - 1], resultSet.getObject(i));
                    }
                    objects.add(map);
                }
            }
        } catch (Exception e) {
            Server.getInstance().getLogger().error("执行 " + commands + " 语句出现异常", e);
        }
        return objects;
    }


    /**
     * 判断数据是否存在
     *
     * @param tableName 表单名称
     * @param column 字段名
     * @param data   数据
     * @return 是否存在
     */
    public static boolean isExists(LoginPool loginPool, String tableName, String column, String data) {
        String sql = "SELECT 1 FROM " + tableName + " WHERE " + column + " = ? LIMIT 1";
        return selectExecute(loginPool, sql, new ChunkSqlType(1, data)).size() > 0;
    }

    /**
     * 执行SQL语句
     *
     * @param sql   SQL 语句
     * @param value 防SQL注入
     * @return 是否执行成功
     * 通过线程池调用 Connection
     */
    public static boolean executeSql(LoginPool loginPool, String sql, ChunkSqlType... value) {
        try (Connection connection = loginPool.dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            bindParameters(preparedStatement, value);
            preparedStatement.execute();
            return true;

        } catch (SQLException e) {
            Server.getInstance().getLogger().error("执行 " + sql + " 语句出现异常", e);
        }
        return false;
    }

    /**
     * 单执行MySQL函数
     *
     * @param functions 封装的函数
     * @return 返回值
     */
    public static SqlData executeFunction(LoginPool loginPool, MySqlFunctions functions) {
        return selectExecute(loginPool, functions.getCommand()).get();
    }

    /**
     * 单执行MySQL函数
     *
     * @param functions 自定义的函数 例如 COUNT(*)
     * @return 返回值
     */
    public static SqlData executeFunction(LoginPool loginPool, String functions) {
        return selectExecute(loginPool, functions).get();
    }

    private static String getUpDataWhere(SqlData data) {
        StringBuilder builder = new StringBuilder();
        for (String column : data.getData().keySet()) {
            builder.append(column).append(" = ? and");
        }
        String str = builder.toString();
        return str.substring(0, str.length() - 3);
    }

    private static String getUpDataColumn(SqlData data) {
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
     * @param database  数据库名
     */
    public static boolean isTableColumnData(LoginPool loginPool, String database, String tableName) {
        if (tableName == null || "".equalsIgnoreCase(tableName.trim())) {
            throw new NullPointerException();
        }
        if (database == null || "".equalsIgnoreCase(database.trim())) {
            throw new NullPointerException();
        }
        String command = "SELECT 1 FROM information_schema.TABLES WHERE table_schema = ? AND table_name = ? LIMIT 1";
        return selectExecute(loginPool, command, new ChunkSqlType(1, database), new ChunkSqlType(2, tableName)).size() > 0;
    }

    /**
     * 修改数据
     *
     * @param data  数据
     * @param where 参数判断
     */
    public static boolean setData(LoginPool loginPool, String tableName, SqlData data, SqlData where) {
        if (data == null || data.getData().isEmpty() || where == null || where.getData().isEmpty()) {
            return false;
        }
        ArrayList<ChunkSqlType> objects = new ArrayList<>();
        int i = 1;
        for (Map.Entry<String, Object> data1 : data.getData().entrySet()) {
            objects.add(new ChunkSqlType(i, data1.getValue()));
            i++;
        }
        for (Map.Entry<String, Object> data1 : where.getData().entrySet()) {
            objects.add(new ChunkSqlType(i, data1.getValue()));
            i++;
        }

        String sql = "UPDATE " + tableName + " SET " + getUpDataColumn(data) + " WHERE " + getUpDataWhere(where);
        return executeSql(loginPool, sql, objects.toArray(new ChunkSqlType[]{}));
    }

    /**
     * 添加数据
     *
     * @param data 数据
     * @param tableName 表单名称
     * @return 是否添加成功
     */
    public static boolean insertData(LoginPool loginPool, String tableName, SqlData data) {
        List<String> columns = data.getColumns();
        List<Object> objects = data.getObjects();
        String sql = buildInsertSql(tableName, columns);
        ArrayList<ChunkSqlType> chunkSqlTypes = new ArrayList<>();
        int i = 1;
        for (Object o : objects) {
            chunkSqlTypes.add(new ChunkSqlType(i, o));
            i++;
        }
        return executeSql(loginPool, sql, chunkSqlTypes.toArray(new ChunkSqlType[0]));
    }

    /**
     * 添加多条数据
     *
     * @param datas     数据列表
     * @param tableName 表单名称
     * @return 是否添加成功
     */
    public static boolean insertData(LoginPool loginPool, String tableName, LinkedList<SqlData> datas) {
        if (datas == null || datas.isEmpty()) {
            return true;
        }
        SqlData first = datas.getFirst();
        List<String> columns = first.getColumns();
        String sql = buildInsertSql(tableName, columns);
        Connection connection = null;
        boolean originalAutoCommit = true;
        try {
            connection = loginPool.dataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                int batchCount = 0;
                for (SqlData data : datas) {
                    List<String> rowColumns = data.getColumns();
                    if (!columns.equals(rowColumns)) {
                        rollbackQuietly(connection);
                        return false;
                    }
                    int index = 1;
                    List<Object> objects = data.getObjects();
                    for (Object value : objects) {
                        preparedStatement.setObject(index, ChunkSqlType.normalizeSqlValue(value));
                        index++;
                    }
                    preparedStatement.addBatch();
                    batchCount++;
                    if (batchCount == BATCH_SIZE) {
                        preparedStatement.executeBatch();
                        preparedStatement.clearBatch();
                        batchCount = 0;
                    }
                }
                if (batchCount > 0) {
                    preparedStatement.executeBatch();
                    preparedStatement.clearBatch();
                }
            }
            connection.commit();
            return true;
        } catch (SQLException e) {
            rollbackQuietly(connection);
            Server.getInstance().getLogger().error("执行 " + sql + " 语句出现异常", e);
        } finally {
            restoreAutoCommitQuietly(connection, originalAutoCommit);
            closeQuietly(connection);
        }
        return false;
    }

    private static String buildInsertSql(String tableName, List<String> columns) {
        String column = String.join(",", columns);
        StringBuilder builder = new StringBuilder("INSERT INTO ").append(tableName).append(" (").append(column).append(") VALUES (");
        if (!columns.isEmpty()) {
            builder.append("?");
            for (int i = 1; i < columns.size(); i++) {
                builder.append(",?");
            }
        }
        builder.append(")");
        return builder.toString();
    }

    private static void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException ignore) {
        }
    }

    private static void restoreAutoCommitQuietly(Connection connection, boolean originalAutoCommit) {
        if (connection == null) {
            return;
        }
        try {
            connection.setAutoCommit(originalAutoCommit);
        } catch (SQLException ignore) {
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignore) {
        }
    }

    /**
     * 删除数据
     *
     * @param tableName 表单名称
     * @param data      数据
     * @return 是否删除成功
     */
    public static boolean deleteData(LoginPool loginPool, String tableName, SqlData data) {
        StringBuilder cmd = new StringBuilder("DELETE FROM " + tableName + " WHERE ");
        ArrayList<ChunkSqlType> objects = new ArrayList<>();
        int i = 0;
        for (java.lang.String column : data.getData().keySet()) {
            if (i >= 1) {
                cmd.append(" and ").append(column).append(" = ?");
            } else {
                cmd.append(column).append(" = ?");
            }
            objects.add(new ChunkSqlType(i + 1, data.getData().get(column)));
            i++;
        }

        return executeSql(loginPool, cmd.toString(), objects.toArray(new ChunkSqlType[]{}));
    }

    private static void bindParameters(PreparedStatement preparedStatement, ChunkSqlType... value) throws SQLException {
        for (ChunkSqlType type : value) {
            preparedStatement.setObject(type.getI(), type.getSqlValue());
        }
    }
}

package com.smallaswater.easysqlx.mysql;


import cn.nukkit.plugin.Plugin;
import com.smallaswater.easysqlx.EasySQLX;
import com.smallaswater.easysqlx.exceptions.MySqlLoginException;
import com.smallaswater.easysqlx.common.data.SqlData;
import com.smallaswater.easysqlx.common.data.SqlDataList;
import com.smallaswater.easysqlx.mysql.utils.*;
import com.smallaswater.easysqlx.mysql.data.SqlDataManager;
import com.smallaswater.easysqlx.mysql.manager.PluginManager;
import com.smallaswater.easysqlx.mysql.utils.SelectType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

/**
 * 数据库基类文件
 *
 * @author 楠木i，若水
 */
public abstract class BaseMySql {

    private final Plugin plugin;
    private final UserData data;
    private final String connectionParameters;

    protected LoginPool pool;

    public BaseMySql(@NotNull Plugin plugin, @NotNull UserData data) {
        this(plugin, data, null);
    }

    public BaseMySql(@NotNull Plugin plugin, @NotNull UserData data, String connectionParameters) {
        this.plugin = plugin;
        this.data = data;
        if (connectionParameters == null || connectionParameters.trim().isEmpty()) {
            this.connectionParameters = "&autoReconnect=true&failOverReadOnly=false&serverTimezone=GMT&characterEncoding=utf8&useSSL=false";
        }else {
            this.connectionParameters = connectionParameters;
        }
    }

    public static String getDefaultConfig() {
        return getDefaultTable(new TableType("name", DataType.getVARCHAR()), new TableType("config", DataType.getTEXT()));
    }

    public static String getDefaultTable(TableType... type) {
        StringBuilder builder = new StringBuilder();
        int i = 1;
        for (TableType type1 : type) {
            builder.append(type1.toTable());
            if (i != type.length) {
                builder.append(",");
            }
            i++;
        }
        return builder.toString();
    }

    /**
     * 连接数据库
     *
     * @return 是否链接成功
     * @throws MySqlLoginException 连接错误
     */
    protected boolean connect() throws MySqlLoginException {
        Connection connection = null;
        try {
            this.pool = EasySQLX.getLoginPool(data);
            this.pool.setManager(this);
            Class.forName("com.mysql.cj.jdbc.Driver");
            HikariConfig config = new HikariConfig();
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl("jdbc:mysql://" + this.data.getHost() + ':' + this.data.getPort() + '/' + this.data.getDatabase() + "?" + this.connectionParameters);
            config.setUsername(this.data.getUser());
            config.setPassword(this.data.getPassWorld());
            this.pool.dataSource = new HikariDataSource(config);
            /*this.pool.dataSource.setInitialSize(3);
            this.pool.dataSource.setMinIdle(1);
            this.pool.dataSource.setMaxActive(30);
            this.pool.dataSource.setValidationQuery("SELECT 1");
            this.pool.dataSource.setTimeBetweenEvictionRunsMillis(180000);
            this.pool.dataSource.setBreakAfterAcquireFailure(true);
            this.pool.dataSource.setTimeBetweenConnectErrorMillis(180000);
            this.pool.dataSource.setConnectionErrorRetryAttempts(3);
            this.pool.dataSource.addFilters("wall");
*/
            //TODO 修复链接判断
            connection = this.getConnection();
            if (connection != null && !connection.isClosed()) {
                this.plugin.getLogger().info("已连接数据库");
                PluginManager.connect(plugin, this);
                return true;
            } else {
                plugin.getLogger().info("无法连接数据库");
            }
        } catch (Exception e) {
            plugin.getLogger().error("连接数据库出现异常...", e);
            this.shutdown();
        }
        throw new MySqlLoginException();
    }

    public Connection getConnection() {
        try {
            return pool.dataSource.getConnection();
        } catch (Exception e) {
            EasySQLX.getInstance().getLogger().error("获取数据库连接出现异常...", e);
            return null;
        }
    }

    /**
     * 关闭数据库连接
     */
    public void shutdown() {
        if (this.pool != null) {
            this.pool.dataSource.close();
            this.plugin.getLogger().info("已断开数据库连接");
        }
    }

    /**
     * 执行sql语句
     *
     * @param sql sql语句
     * @param value 参数
     * @return 是否执行成功
     */
    public boolean executeSql(String sql, ChunkSqlType... value) {
        return SqlDataManager.executeSql(this.pool, sql, value);
    }

    /**
     * 单执行MySQL函数
     *
     * @param functions 封装的函数
     * @return 返回值
     */
    public SqlData executeFunction(MySqlFunctions functions) {
        return SqlDataManager.executeFunction(this.pool, functions.getCommand());
    }

    /**
     * 单执行MySQL函数
     *
     * @param functions 自定义的函数 例如 COUNT(*)
     * @return 返回值
     */
    public SqlData executeFunction(String functions) {
        return SqlDataManager.executeFunction(this.pool, functions);
    }

    /**
     * 是否存在表
     *
     * @param tableName 表名称
     * @return 是否存在
     */
    public boolean isExistTable(@NotNull String tableName) {
        try {
            ResultSet resultSet = this.getConnection().getMetaData().getTables(null, null, conversionTableName(tableName), null);
            return resultSet.next();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 创建表单
     *
     * @param tableName 表名称
     * @return 是否创建成功
     */
    public boolean createTable(@NotNull String tableName) {
        return this.createTable(conversionTableName(tableName), new TableType("id", DataType.getID()));
    }

    /**
     * 创建表单
     *
     * @param tableName 表名称
     * @param tableTypes 参数
     * @return 是否创建成功
     */
    public boolean createTable(@NotNull String tableName, TableType... tableTypes) {
        if (!this.isExistTable(tableName)) {
            StringBuilder index = new StringBuilder();
            for (TableType tableType : tableTypes) {
                if (tableType.isIndex()) {
                    index.append(", INDEX(").append(tableType.getName()).append(")");
                }
            }
            String command = "CREATE TABLE " + conversionTableName(tableName) + "(" + getDefaultTable(tableTypes) + index + ")engine=InnoDB default charset=utf8";
            return this.executeSql(command);
        }
        return false;
    }

    /**
     * 删除表
     *
     * @param tableName 表名称
     */
    public void deleteTable(@NotNull String tableName) {
        this.executeSql("DROP TABLE " + conversionTableName(tableName));
    }

    /**
     * 是否存在字段
     *
     * @param tableName  表名
     * @param column 字段名
     * @return 是否存在
     */
    public boolean isExistColumn(@NotNull String tableName, @NotNull String column) {
        try {
            ResultSet resultSet = this.getConnection().getMetaData().getColumns(null, null, conversionTableName(tableName), column);
            return resultSet.next();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 给表增加字段
     *
     * @param tableName  表单名
     * @param tableType 字段参数
     * @return 是否成功
     */
    public boolean createColumn(@NotNull String tableName, @NotNull TableType tableType) {
        String command = "ALTER TABLE " + conversionTableName(tableName) + " ADD " + tableType.toString();
        return this.executeSql(command);
    }

    /**
     * 给表删除字段
     *
     * @param args 字段名
     * @param tableName 表单名称
     * @return 删除一个字段
     */
    public boolean deleteColumn(@NotNull String tableName, String args) {
        return this.executeSql("ALTER TABLE " + conversionTableName(tableName) + " DROP ?", new ChunkSqlType(1, args));
    }

    /**
     * 是否有数据
     *
     * @param tableName 表名称
     * @param column 条件:字段
     * @param data 条件:值
     * @return 是否存在数据
     */
    public boolean isExistsData(@NotNull String tableName, @NotNull String column, @NotNull String data) {
        return SqlDataManager.isExists(this.pool, conversionTableName(tableName), column, data);
    }

    /**
     * 修改数据
     *
     * @param tableName 表单名称
     * @param data  数据
     * @param where 参数判断
     * @return 是否修改成功
     */
    public boolean setData(@NotNull String tableName, @NotNull SqlData data, @NotNull SqlData where) {
        return SqlDataManager.setData(this.pool, conversionTableName(tableName), data, where);
    }

    /**
     * 添加数据
     *
     * @param tableName 表单名称
     * @param data 数据
     * @return 是否添加成功
     */
    public boolean insertData(@NotNull String tableName, @NotNull SqlData data) {
        return SqlDataManager.insertData(this.pool, conversionTableName(tableName), data);
    }

    /**
     * 添加多条数据
     *
     * @param tableName 表单名称
     * @param datas     数据列表
     * @return 是否添加成功
     */
    public boolean insertData(@NotNull String tableName, @NotNull LinkedList<SqlData> datas) {
        return SqlDataManager.insertData(this.pool, conversionTableName(tableName), datas);
    }

    /**
     * 删除数据
     *
     * @param tableName 表单名称
     * @param data 数据
     * @return 是否删除成功
     */
    public boolean deleteData(@NotNull String tableName, @NotNull SqlData data) {
        return SqlDataManager.deleteData(this.pool, conversionTableName(tableName), data);
    }

    /**
     * 获取数据条数
     *
     * @param sql 条件，例如id=?
     * @param tableName 表单名称
     * @param sqlType 参数
     */
    public int getDataSize(@NotNull String sql, @NotNull String tableName, ChunkSqlType... sqlType) {
        int i = 0;
        Connection connection = this.getConnection();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM " + conversionTableName(tableName) + " " + sql);
            for(ChunkSqlType type: sqlType) {
                preparedStatement.setString(type.getI(),type.getValue());
            }
            resultSet = preparedStatement.executeQuery();
            if (resultSet != null) {
                if (resultSet.next()) {
                    i = resultSet.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return i;
    }

    /**
     * 获取没有重复的数据
     *
     * @param tableName 表名称
     * @param column 要查询的字段
     * @param data 查询条件内容
     * @return 数据
     */
    public SqlDataList<SqlData> getDisTinctData(@NotNull String tableName, String column, @NotNull SqlData data) {
        if (column == null || column.trim().isEmpty()) {
            column = "*";
        }
        return getData(tableName, "DISTINCT " + column, data);
    }


    /**
     * 获取数据
     *
     * @param tableName 表名称
     * @param selectType 查询条件
     * @return 数据
     */
    public SqlDataList<SqlData> getData(@NotNull String tableName, @NotNull SelectType selectType) {
        ArrayList<ChunkSqlType> chunkSqlTypes = new ArrayList<>();
        chunkSqlTypes.add(new ChunkSqlType(1, selectType.getValue()));
        String command = "SELECT * FROM " + conversionTableName(tableName) + " " + selectType;
        return this.getData(command, chunkSqlTypes.toArray(new ChunkSqlType[0]));
    }

    /**
     * 获取数据
     *
     * @param sql 执行查询SQL指令
     * @param types 参数
     * @return 数据
     */
    public SqlDataList<SqlData> getData(@NotNull String sql, ChunkSqlType... types) {
        return SqlDataManager.selectExecute(this.pool, sql, types);
    }

    /**
     * 获取数据
     *
     * @param tableName 表名称
     * @param column 要查询的字段
     * @param data 查询条件内容
     * @return 数据
     */
    public SqlDataList<SqlData> getData(@NotNull String tableName, String column, @NotNull SqlData data) {
        if (column == null || column.trim().isEmpty()) {
            column = "*";
        }
        ArrayList<ChunkSqlType> chunkSqlTypes = new ArrayList<>();
        StringBuilder sqlCommand = new StringBuilder();
        int i = 1;
        for(Map.Entry<String, Object> sqlData: data.getData().entrySet()) {
            if (i > 1) {
                sqlCommand.append(" AND ");
            }
            sqlCommand.append(sqlData.getKey()).append("=?");
            chunkSqlTypes.add(new ChunkSqlType(i, sqlData.getValue().toString()));
            i++;
        }
        String command = "SELECT " + column + " FROM " + conversionTableName(tableName) + " WHERE " + sqlCommand;
        return this.getData(command, chunkSqlTypes.toArray(new ChunkSqlType[0]));
    }

    /**
     * 替换特殊符号并添加`符号，规避mysql保留字
     *
     * @param tableName 表名
     * @return 处理后的表名
     */
    @NotNull
    private static String conversionTableName(@NotNull String tableName) {
        if (tableName.startsWith("`") && tableName.endsWith("`")) {
            tableName = tableName.substring(1, tableName.length() - 1);
        }
        return "`" + tableName.trim().replaceAll("\\\\", "\\\\\\\\")
                .replace("_", "\\_").replace("'", "\\'")
                .replace("%", "\\%").replace("*", "\\*") + "`";
    }


}

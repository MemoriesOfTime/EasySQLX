package com.smallaswater.easysql.v3.mysql;


import cn.nukkit.plugin.Plugin;
import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import com.smallaswater.easysql.EasySql;
import com.smallaswater.easysql.exceptions.MySqlLoginException;
import com.smallaswater.easysql.mysql.data.SqlData;
import com.smallaswater.easysql.mysql.data.SqlDataList;
import com.smallaswater.easysql.mysql.utils.*;
import com.smallaswater.easysql.v3.mysql.data.SqlDataManager;
import com.smallaswater.easysql.v3.mysql.manager.PluginManager;
import com.smallaswater.easysql.v3.mysql.utils.SelectType;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * 数据库基类文件
 *
 * @author 楠木i，若水
 */
public abstract class BaseMySql {

    private final UserData data;

    private final Plugin plugin;

    protected LoginPool pool;

    public BaseMySql(@NotNull Plugin plugin, @NotNull UserData data) {
        this.data = data;
        this.plugin = plugin;
    }

    public static String getDefaultConfig() {
        return getDefaultTable(new TableType("name", Types.VARCHAR), new TableType("config", Types.TEXT));
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
            this.pool = EasySql.getLoginPool(data);
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.pool.dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            this.pool.dataSource.setUrl("jdbc:mysql://" + this.data.getHost() + ':' + this.data.getPort() + '/' + this.data.getDatabase() + "?&autoReconnect=true&failOverReadOnly=false&serverTimezone=GMT&characterEncoding=utf8&useSSL=false");
            this.pool.dataSource.setUsername(this.data.getUser());
            this.pool.dataSource.setPassword(this.data.getPassWorld());
            this.pool.dataSource.setInitialSize(3);
            this.pool.dataSource.setMinIdle(1);
            this.pool.dataSource.setMaxActive(30);
            this.pool.dataSource.setValidationQuery("SELECT 1");
            this.pool.dataSource.setTimeBetweenEvictionRunsMillis(1800);
            this.pool.dataSource.setBreakAfterAcquireFailure(true);
            this.pool.dataSource.setTimeBetweenConnectErrorMillis(1800);
            this.pool.dataSource.setConnectionErrorRetryAttempts(3);
            this.pool.dataSource.addFilters("wall");

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
            e.printStackTrace();
            plugin.getLogger().info("连接数据库出现异常...");
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        throw new MySqlLoginException();
    }

    /**
     * 启用WallFilter
     *
     * @return 是否启用成功
     */
    public boolean enableWallFilter() {
        for (Filter filter : this.pool.dataSource.getProxyFilters()) {
            if (filter instanceof WallFilter) {
                WallFilter wallFilter = (WallFilter) filter;
                WallConfig config = new WallConfig();
                wallFilter.setConfig(config);
                wallFilter.init(this.pool.dataSource);
                return wallFilter.isInited();
            }
        }
        return false;
    }

    /**
     * 获取WallFilter配置
     *
     * @return WallFilter配置
     */
    public WallConfig getWallFilterConfig() {
        for (Filter filter : this.pool.dataSource.getProxyFilters()) {
            if (filter instanceof WallFilter) {
                WallFilter wallFilter = (WallFilter) filter;
                return wallFilter.getConfig();
            }
        }
        return null;
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
     * 关闭数据库连接
     */
    public void shutdown() {
        if (this.pool != null) {
            this.pool.dataSource.close();
            this.plugin.getLogger().info(" 已断开数据库连接");
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
    public boolean isExistTable(String tableName) {
        try {
            ResultSet resultSet = this.getConnection().getMetaData().getTables(null, null, tableName, null);
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
    public boolean createTable(String tableName) {
        return this.createTable(tableName, new TableType("id", Types.ID));
    }

    /**
     * 创建表单
     *
     * @param tableName 表名称
     * @param tableTypes 参数
     * @return 是否创建成功
     */
    public boolean createTable(String tableName, TableType... tableTypes) {
        if (!this.isExistTable(tableName)) {
            String command = "CREATE TABLE " + tableName + "(" + getDefaultTable(tableTypes) + ")engine=InnoDB default charset=utf8";
            return this.executeSql(command);
        }
        return false;
    }

    /**
     * 删除表
     *
     * @param tableName 表名称
     */
    public void deleteTable(String tableName) {
        String sql = "DROP TABLE " + tableName;
        this.executeSql(sql);
    }

    /**
     * 是否存在字段
     *
     * @param table  表名
     * @param column 字段名
     * @return 是否存在
     */
    public boolean isExistColumn(String table, String column) {
        try {
            ResultSet resultSet = this.getConnection().getMetaData().getColumns(null, null, table, column);
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
    public boolean createColumn(String tableName, TableType tableType) {
        String command = "ALTER TABLE " + tableName + " ADD " + tableType.getName() + " " + tableType.getType().toString();
        return this.executeSql(command);
    }

    /**
     * 给表删除字段
     *
     * @param args 字段名
     * @param tableName 表单名称
     * @return 删除一个字段
     */
    public boolean deleteColumn(String tableName, String args) {
        String command = "ALTER TABLE " + tableName + " DROP ?";
        return this.executeSql(command, new ChunkSqlType(1, args));
    }

    /**
     * 是否有数据
     *
     * @param tableName 表名称
     * @param column 条件:字段
     * @param data 条件:值
     * @return 是否存在数据
     */
    public boolean isExistsData(String tableName, String column, String data) {
        return SqlDataManager.isExists(this.pool, tableName, column, data);
    }

    /**
     * 修改数据
     *
     * @param tableName 表单名称
     * @param data  数据
     * @param where 参数判断
     * @return 是否修改成功
     */
    public boolean setData(String tableName, SqlData data, SqlData where) {
        return SqlDataManager.setData(this.pool, tableName, data, where);
    }

    /**
     * 添加数据
     *
     * @param tableName 表单名称
     * @param data 数据
     * @return 是否添加成功
     */
    public boolean insertData(String tableName, SqlData data) {
        return SqlDataManager.insertData(this.pool, tableName, data);
    }

    /**
     * 添加多条数据
     *
     * @param tableName 表单名称
     * @param datas     数据列表
     * @return 是否添加成功
     */
    public boolean insertData(String tableName, LinkedList<SqlData> datas) {
        return SqlDataManager.insertData(this.pool, tableName, datas);
    }

    /**
     * 删除数据
     *
     * @param tableName 表单名称
     * @param data 数据
     * @return 是否删除成功
     */
    public boolean deleteData(String tableName, SqlData data) {
        return SqlDataManager.deleteData(this.pool, tableName, data);
    }

    /**
     * 获取数据条数
     */
    public int getDataSize(String sql, String tableName, ChunkSqlType... sqlType) {
        int i = 0;
        Connection connection = this.getConnection();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName + " " + sql);
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
     * 获取数据
     *
     * @param tableName 表名称
     * @param selectType 查询条件
     * @return 数据
     */
    public SqlDataList<SqlData> getData(String tableName, SelectType selectType) {
        ArrayList<ChunkSqlType> chunkSqlTypes = new ArrayList<>();
        chunkSqlTypes.add(new ChunkSqlType(1, selectType.getValue()));
        String command = "SELECT * FROM " + tableName + " " + selectType;
        return this.getData(command, chunkSqlTypes.toArray(new ChunkSqlType[0]));
    }

    /**
     * 获取数据
     *
     * @param sql 执行查询SQL指令
     * @param types 参数
     * @return 数据
     */
    public SqlDataList<SqlData> getData(String sql, ChunkSqlType... types) {
        return SqlDataManager.selectExecute(this.pool, sql, types);
    }

}

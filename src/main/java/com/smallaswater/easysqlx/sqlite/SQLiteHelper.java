package com.smallaswater.easysqlx.sqlite;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.smallaswater.easysqlx.EasySQLX;
import com.smallaswater.easysqlx.common.data.SqlData;

import java.lang.reflect.Field;
import java.sql.*;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author Sobadfish
 */
public class SQLiteHelper {

    private Connection connection;

    private Statement statement;

    private final String dbFilePath;

    /**
     * PreparedStatement 缓存
     * 缓存可能常用的语句预编译
     */
    private final Cache<String, PreparedStatement> preparedStatementCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, java.util.concurrent.TimeUnit.MINUTES)
            .removalListener(notification -> {
                if (notification.getValue() instanceof PreparedStatement) {
                    try {
                        ((PreparedStatement) notification.getValue()).close();
                    } catch (SQLException e) {
                        EasySQLX.getInstance().getLogger().error("关闭 PreparedStatement 时异常 ", e);
                    }
                }
            })
            .build();

    /**
     * 构造函数
     *
     * @param dbFilePath sqlite db 文件路径
     */
    public SQLiteHelper(String dbFilePath) throws ClassNotFoundException, SQLException {
        this.dbFilePath = dbFilePath;
        this.connection = getConnection(dbFilePath);
    }

    /**
     * 获取数据库连接
     *
     * @param dbFilePath db文件路径
     * @return 数据库连接
     */
    private Connection getConnection(String dbFilePath) throws ClassNotFoundException, SQLException {
        Connection conn = null;
        // 1、加载驱动
        Class.forName("org.sqlite.JDBC");
        // 2、建立连接
        // 注意：此处有巨坑，如果后面的 dbFilePath 路径太深或者名称太长，则建立连接会失败
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
        return conn;
    }


    public boolean exists(String table) {
        try {
            String query = "select * from " + table;
            PreparedStatement statement = this.preparedStatementCache.getIfPresent(query);
            if (statement == null) {
                statement = this.connection.prepareStatement(query);
                this.preparedStatementCache.put(query, statement);
            }
            ResultSet resultSet = statement.executeQuery();

            return true;
        } catch (Exception e) {
            return false;
        }

    }

    public void addTable(String tableName, DBTable tables) {
        if (!exists(tableName)) {
            String sql = "create table " + tableName + "(" + tables.asSql() + ")";
            try {
                getStatement().execute(sql);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getDbFilePath() {
        return dbFilePath;
    }

    /**
     * 增加数据
     */
    public <T> void add(String tableName, T values) {
        try {
            SqlData sqlData = SqlData.classToSqlData(values);
            this.add(tableName, sqlData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 增加数据
     */
    public SQLiteHelper add(String tableName, SqlData values) {
        try {
            String sql = "insert into " + tableName + "(" + values.getColumnToString() + ") values (" + values.getObjectToString() + ")";
            this.getStatement().execute(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * 删除数据
     */
    public SQLiteHelper remove(String tableName, int id) {
        try {
            String sql = "delete from " + tableName + " where id = " + id;
            this.getStatement().execute(sql);
        } catch (Exception ignore) {
        }
        return this;
    }

    public SQLiteHelper remove(String tableName, String key, String value) {
        try {
            String sql = "delete from " + tableName + " where " + key + " = ?";
            PreparedStatement statement = this.preparedStatementCache.getIfPresent(sql);
            if (statement == null) {
                statement = this.connection.prepareStatement(sql);
                this.preparedStatementCache.put(sql, statement);
            }
            statement.setString(1, value);
            statement.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public SQLiteHelper removeAll(String tableName) {
        try {
            String sql = "delete from " + tableName;
            this.getStatement().execute(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public <T> SQLiteHelper set(String tableName, T values) {
        SqlData contentValues = SqlData.classToSqlDataAsId(values);
        if (contentValues.getInt("id") == -1) {
            throw new NullPointerException("无 id 信息");
        }
        return set(tableName, contentValues.getInt("id"), contentValues);
    }

    public <T> SQLiteHelper set(String tableName, String key, String value, T values) {
        SqlData sqlData = SqlData.classToSqlData(values);
        return set(tableName, key, value, sqlData);
    }

    /**
     * 更新数据
     */
    public SQLiteHelper set(String tableName, int id, SqlData values) {
        try {
            this.getStatement().execute(MessageFormat.format("update {0} set {1} where id = {2}", tableName, values.toUpdateValue(), id));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * 更新数据
     */
    public SQLiteHelper set(String tableName, String key, String value, SqlData values) {
        try {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, Object> entry : values.getData().entrySet()) {
                if ("id".equalsIgnoreCase(entry.getKey())) {
                    continue;
                }
                builder.append(entry.getKey()).append(" = ?,");
            }
            String str = builder.toString();
            str = str.substring(0, str.length() - 1);

            String sql = "update " + tableName + " set " + str + " where " + key + " = ?";
            PreparedStatement statement = this.preparedStatementCache.getIfPresent(sql);
            if (statement == null) {
                statement = this.connection.prepareStatement(sql);
                this.preparedStatementCache.put(sql, statement);
            }

            int index = 0;
            for (Map.Entry<String, Object> entry : values.getData().entrySet()) {
                statement.setString(++index, entry.getValue().toString());
            }
            statement.setString(++index, value);

            statement.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;

    }

    public <T> SQLiteHelper set(String tableName, SqlData key, T values) {
        SqlData sqlData = SqlData.classToSqlData(values);
        try {
            String sql = "update " + tableName + " set " + sqlData.toUpdateValue() + " where " + getUpDataWhere(key);
            try (PreparedStatement statement = this.getConnection().prepareStatement(sql)) {
                int i = 1;
                for (Object type : key.getObjects()) {
                    statement.setString(i, type.toString());
                    i++;
                }
                statement.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    /**
     * 判断是否存在数据
     *
     * @param tableName 表名
     * @param key     查询条件 键
     * @param value   查询条件 值
     * @return 是否存在数据
     */
    public boolean hasData(String tableName, String key, String value) {
        try {
            String query = "SELECT COUNT(*) FROM " + tableName + " WHERE " + key + " = ?";
            PreparedStatement statement = this.preparedStatementCache.getIfPresent(query);
            if (statement == null) {
                statement = this.connection.prepareStatement(query);
                this.preparedStatementCache.put(query, statement);
            }
            statement.setString(1, value);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                resultSet.close();
                return count > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
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

    public <T> T get(String tableName, int id, Class<T> clazz) {
        T instance = null;
        try {
            String query = "SELECT * FROM " + tableName + " WHERE id = ?";
            PreparedStatement statement = this.preparedStatementCache.getIfPresent(query);
            if (statement == null) {
                statement = this.connection.prepareStatement(query);
                this.preparedStatementCache.put(query, statement);
            }
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();

            instance = explainClass(resultSet, clazz, clazz.newInstance());
            resultSet.close();
        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return instance;
    }

    public <T> T get(String tableName, String key, String value, Class<T> clazz) {
        T instance = null;
        try {
            // 准备 SQL 查询语句
            String query = "SELECT * FROM " + tableName + " WHERE " + key + " = ?";
            PreparedStatement statement = this.preparedStatementCache.getIfPresent(query);
            if (statement == null) {
                statement = this.connection.prepareStatement(query);
                this.preparedStatementCache.put(query, statement);
            }
            statement.setString(1, value);

            ResultSet resultSet = statement.executeQuery();
            T t = clazz.newInstance();

            instance = explainClass(resultSet, clazz, t);
            resultSet.close();
        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return instance;
    }

    public <T> LinkedList<T> getDataByString(String tableName, String selection, String[] key, Class<T> clazz) {
        LinkedList<T> datas = new LinkedList<>();
        try {
            // 准备 SQL 查询语句
            String query = "SELECT * FROM " + tableName + " WHERE " + selection;
            PreparedStatement statement = this.preparedStatementCache.getIfPresent(query);
            if (statement == null) {
                statement = this.connection.prepareStatement(query);
                this.preparedStatementCache.put(query, statement);
            }

            // 设置查询条件
            for (int i = 0; i < key.length; i++) {
                statement.setString(i + 1, key[i]);
            }

            // 执行查询
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                T t = clazz.newInstance();
                datas.add(explainClass(resultSet, clazz, t));
            }
            resultSet.close();

        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return datas;
    }


    public <T> LinkedList<T> getAll(String tableName, Class<T> clazz) {
        LinkedList<T> datas = new LinkedList<>();
        try {
            // 准备 SQL 查询语句
            String query = "SELECT * FROM " + tableName;
            PreparedStatement statement = this.preparedStatementCache.getIfPresent(query);
            if (statement == null) {
                statement = this.connection.prepareStatement(query);
                this.preparedStatementCache.put(query, statement);
            }

            // 执行查询
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                T t = clazz.newInstance();
                datas.add(explainClass(resultSet, clazz, t));
            }

            resultSet.close();

        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return datas;
    }

    private <T> T explainClass(ResultSet cursor, Class<?> tc, T t) {
        try {
            ResultSetMetaData rsmd = cursor.getMetaData();
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                String name = rsmd.getColumnName(i + 1);
                Field field = tc.getField(name);
                if (field.getType() == int.class) {
                    field.set(t, cursor.getInt(name));
                } else if (field.getType() == float.class || field.getType() == double.class) {
                    field.set(t, cursor.getFloat(name));
                } else if (field.getType() == boolean.class) {
                    field.set(t, Boolean.valueOf(cursor.getString(name)));
                } else if (field.getType() == long.class) {
                    field.set(t, cursor.getLong(name));
                } else {
                    field.set(t, cursor.getString(name));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }


    public static class DBTable {
        LinkedHashMap<String, String> tables = new LinkedHashMap<>();

        public DBTable(String key, String value) {
            tables.put(key, value);
        }

        public DBTable(Map<String, String> m) {
            tables.putAll(m);
        }

        public DBTable put(String key, String value) {
            tables.put(key, value);
            return this;
        }

        public String asSql() {
            StringBuilder s = new StringBuilder();
            for (Map.Entry<String, String> e : tables.entrySet()) {
                s.append(e.getKey()).append(" ").append(e.getValue()).append(",");
            }
            return s.substring(0, s.length() - 1);

        }

        public static DBTable asDbTable(Class<?> t) {
            Field[] fields = t.getFields();
            LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
            boolean isId = false;
            // 先找自增id
            for (Field field : fields) {
                if ("id".equalsIgnoreCase(field.getName()) && field.getType() == long.class) {
                    //找到了
                    isId = true;
                    break;
                }
            }
            if (!isId) {
                throw new NullPointerException("数据库类需要一个id");
            }
            linkedHashMap.put("id", "integer primary key autoincrement");
            for (Field field : fields) {
                if ("id".equalsIgnoreCase(field.getName()) && field.getType() == long.class) {
                    continue;
                }
                if (field.getType() == float.class || field.getType() == double.class) {
                    linkedHashMap.put(field.getName().toLowerCase(), field.getType().getName());
                } else {
                    linkedHashMap.put(field.getName().toLowerCase(), "varchar(20)");
                }
            }
            return new DBTable(linkedHashMap);
        }

    }


    private Connection getConnection() throws ClassNotFoundException, SQLException {
        if (this.connection == null) {
            this.connection = getConnection(dbFilePath);
        }
        return this.connection;
    }

    private Statement getStatement() throws SQLException, ClassNotFoundException {
        if (this.statement == null) {
            this.statement = getConnection().createStatement();
        }
        return this.statement;
    }

    public void destroyed() {
        this.close();
    }

    /**
     * 数据库资源关闭和释放
     */
    public void close() {
        try {
            if (null != connection) {
                connection.close();
                connection = null;
            }

            if (null != statement) {
                statement.close();
                statement = null;
            }

            this.preparedStatementCache.invalidateAll();
        } catch (SQLException e) {
            EasySQLX.getInstance().getLogger().error("Sqlite数据库关闭时异常 ", e);
        }
    }

}

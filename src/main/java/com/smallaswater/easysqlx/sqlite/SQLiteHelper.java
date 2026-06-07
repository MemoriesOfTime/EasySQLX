package com.smallaswater.easysqlx.sqlite;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.smallaswater.easysqlx.EasySQLX;
import com.smallaswater.easysqlx.common.data.SqlData;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Sobadfish
 */
public class SQLiteHelper {

    private Connection connection;

    private Statement statement;

    private final String dbFilePath;

    private static final int CACHE_MAXIMUM_SIZE = 128;
    private static final int BATCH_SIZE = 500;

    private static final ClassValue<Map<String, Field>> FIELD_CACHE = new ClassValue<Map<String, Field>>() {
        @Override
        protected Map<String, Field> computeValue(Class<?> type) {
            Map<String, Field> fields = new HashMap<>();
            for (Field field : type.getFields()) {
                fields.put(field.getName(), field);
                fields.put(field.getName().toLowerCase(), field);
            }
            return fields;
        }
    };

    /**
     * PreparedStatement 缓存
     * 缓存可能常用的语句预编译
     */
    private final Cache<String, PreparedStatement> preparedStatementCache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_MAXIMUM_SIZE)
            .expireAfterAccess(10, java.util.concurrent.TimeUnit.MINUTES)
            .removalListener(notification -> {
                if (notification.getValue() instanceof PreparedStatement) {
                    try {
                        ((PreparedStatement) notification.getValue()).close();
                    } catch (SQLException e) {
                        logSqlException("关闭 PreparedStatement 时异常 ", e);
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


    public synchronized boolean exists(String table) {
        try {
            String query = "SELECT 1 FROM sqlite_master WHERE type='table' AND name COLLATE NOCASE = ? LIMIT 1";
            PreparedStatement statement = getPreparedStatement(query);
            statement.setObject(1, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception e) {
            return false;
        }

    }

    public synchronized void addTable(String tableName, DBTable tables) {
        String sql = "create table if not exists " + tableName + "(" + tables.asSql() + ")";
        try {
            getStatement().execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getDbFilePath() {
        return dbFilePath;
    }

    /**
     * 增加数据
     */
    public synchronized <T> void add(String tableName, T values) {
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
    public synchronized SQLiteHelper add(String tableName, SqlData values) {
        try {
            List<String> columns = values.getColumns();
            List<Object> objects = values.getObjects();
            String sql = buildInsertSql(tableName, columns);
            PreparedStatement statement = getPreparedStatement(sql);
            bindObjects(statement, objects, 1);
            statement.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public synchronized SQLiteHelper add(String tableName, LinkedList<SqlData> values) {
        if (values == null || values.isEmpty()) {
            return this;
        }
        boolean originalAutoCommit = true;
        PreparedStatement statement = null;
        try {
            Connection connection = getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            List<String> columns = values.getFirst().getColumns();
            String sql = buildInsertSql(tableName, columns);
            statement = getPreparedStatement(sql);
            int batchCount = 0;
            for (SqlData value : values) {
                List<String> rowColumns = value.getColumns();
                if (!columns.equals(rowColumns)) {
                    throw new IllegalArgumentException("批量插入的数据列不一致");
                }
                bindObjects(statement, value.getObjects(), 1);
                statement.addBatch();
                batchCount++;
                if (batchCount == BATCH_SIZE) {
                    statement.executeBatch();
                    statement.clearBatch();
                    batchCount = 0;
                }
            }
            if (batchCount > 0) {
                statement.executeBatch();
                statement.clearBatch();
            }
            connection.commit();
        } catch (Exception e) {
            try {
                getConnection().rollback();
            } catch (Exception ignore) {
            }
            throw new RuntimeException(e);
        } finally {
            if (statement != null) {
                try {
                    statement.clearBatch();
                } catch (Exception ignore) {
                }
            }
            try {
                getConnection().setAutoCommit(originalAutoCommit);
            } catch (Exception ignore) {
            }
        }
        return this;
    }

    /**
     * 删除数据
     */
    public synchronized SQLiteHelper remove(String tableName, int id) {
        try {
            String sql = "delete from " + tableName + " where id = " + id;
            this.getStatement().execute(sql);
        } catch (Exception ignore) {
        }
        return this;
    }

    public synchronized SQLiteHelper remove(String tableName, String key, String value) {
        try {
            String sql = "delete from " + tableName + " where " + key + " = ?";
            PreparedStatement statement = getPreparedStatement(sql);
            statement.setObject(1, value);
            statement.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public synchronized SQLiteHelper removeAll(String tableName) {
        try {
            String sql = "delete from " + tableName;
            this.getStatement().execute(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public synchronized <T> SQLiteHelper set(String tableName, T values) {
        SqlData contentValues = SqlData.classToSqlDataAsId(values);
        if (contentValues.getInt("id") == -1) {
            throw new NullPointerException("无 id 信息");
        }
        return set(tableName, contentValues.getInt("id"), contentValues);
    }

    public synchronized <T> SQLiteHelper set(String tableName, String key, String value, T values) {
        SqlData sqlData = SqlData.classToSqlData(values);
        return set(tableName, key, value, sqlData);
    }

    /**
     * 更新数据
     */
    public synchronized SQLiteHelper set(String tableName, int id, SqlData values) {
        try {
            List<Map.Entry<String, Object>> entries = getUpdateEntries(values);
            if (entries.isEmpty()) {
                return this;
            }
            String sql = "update " + tableName + " set " + getUpdateSetSql(entries) + " where id = ?";
            PreparedStatement statement = getPreparedStatement(sql);
            int index = bindEntries(statement, entries, 1);
            statement.setObject(index, id);
            statement.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * 更新数据
     */
    public synchronized SQLiteHelper set(String tableName, String key, String value, SqlData values) {
        try {
            List<Map.Entry<String, Object>> entries = getUpdateEntries(values);
            if (entries.isEmpty()) {
                return this;
            }
            String sql = "update " + tableName + " set " + getUpdateSetSql(entries) + " where " + key + " = ?";
            PreparedStatement statement = getPreparedStatement(sql);
            int index = bindEntries(statement, entries, 1);
            statement.setObject(index, value);

            statement.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;

    }

    public synchronized <T> SQLiteHelper set(String tableName, SqlData key, T values) {
        SqlData sqlData = SqlData.classToSqlData(values);
        List<Map.Entry<String, Object>> entries = getUpdateEntries(sqlData);
        if (entries.isEmpty()) {
            return this;
        }
        String sql = "update " + tableName + " set " + getUpdateSetSql(entries) + " where " + getUpDataWhere(key);
        try {
            PreparedStatement statement = getPreparedStatement(sql);
            int index = bindEntries(statement, entries, 1);
            bindObjects(statement, key.getObjects(), index);
            statement.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    /**
     * 判断是否存在数据
     *
     * @param tableName 表名
     * @param key       查询条件 键
     * @param value     查询条件 值
     * @return 是否存在数据
     */
    public synchronized boolean hasData(String tableName, String key, String value) {
        try {
            String query = "SELECT 1 FROM " + tableName + " WHERE " + key + " = ? LIMIT 1";
            PreparedStatement statement = getPreparedStatement(query);
            statement.setObject(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUpDataWhere(SqlData data) {
        StringBuilder builder = new StringBuilder();
        for (String column : data.getData().keySet()) {
            builder.append(column).append(" = ? and");
        }
        String str = builder.toString();
        return str.substring(0, str.length() - 3);
    }

    public synchronized <T> T get(String tableName, int id, Class<T> clazz) {
        T instance = null;
        try {
            String query = "SELECT * FROM " + tableName + " WHERE id = ?";
            PreparedStatement statement = getPreparedStatement(query);
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMapper mapper = ResultSetMapper.of(resultSet, clazz);
                if (resultSet.next()) {
                    instance = explainClass(resultSet, clazz.newInstance(), mapper);
                }
            }
        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return instance;
    }

    public synchronized <T> T get(String tableName, String key, String value, Class<T> clazz) {
        T instance = null;
        try {
            // 准备 SQL 查询语句
            String query = "SELECT * FROM " + tableName + " WHERE " + key + " = ?";
            PreparedStatement statement = getPreparedStatement(query);
            statement.setObject(1, value);

            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMapper mapper = ResultSetMapper.of(resultSet, clazz);
                if (resultSet.next()) {
                    T t = clazz.newInstance();
                    instance = explainClass(resultSet, t, mapper);
                }
            }
        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return instance;
    }

    public synchronized <T> LinkedList<T> getDataByString(String tableName, String selection, String[] key, Class<T> clazz) {
        return getDataByString(tableName, selection, key, clazz, 0, 0);
    }

    public synchronized <T> LinkedList<T> getDataByString(String tableName, String selection, String[] key, Class<T> clazz, int start, int length) {
        LinkedList<T> datas = new LinkedList<>();
        try {
            // 准备 SQL 查询语句
            String query = "SELECT * FROM " + tableName + " WHERE " + selection + buildLimitClause(start, length);
            PreparedStatement statement = getPreparedStatement(query);

            // 设置查询条件
            for (int i = 0; i < key.length; i++) {
                statement.setObject(i + 1, key[i]);
            }

            // 执行查询
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMapper mapper = ResultSetMapper.of(resultSet, clazz);
                while (resultSet.next()) {
                    T t = clazz.newInstance();
                    datas.add(explainClass(resultSet, t, mapper));
                }
            }

        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return datas;
    }


    public synchronized <T> LinkedList<T> getAll(String tableName, Class<T> clazz) {
        return getAll(tableName, clazz, 0, 0);
    }

    public synchronized <T> LinkedList<T> getAll(String tableName, Class<T> clazz, int start, int length) {
        LinkedList<T> datas = new LinkedList<>();
        try {
            // 准备 SQL 查询语句
            String query = "SELECT * FROM " + tableName + buildLimitClause(start, length);
            PreparedStatement statement = getPreparedStatement(query);

            // 执行查询
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMapper mapper = ResultSetMapper.of(resultSet, clazz);
                while (resultSet.next()) {
                    T t = clazz.newInstance();
                    datas.add(explainClass(resultSet, t, mapper));
                }
            }

        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return datas;
    }

    private <T> T explainClass(ResultSet cursor, T t, ResultSetMapper mapper) {
        try {
            for (String name : mapper.columns) {
                Field field = mapper.fields.get(name);
                if (field == null) {
                    continue;
                }
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

    private PreparedStatement getPreparedStatement(String sql) throws SQLException {
        PreparedStatement statement = this.preparedStatementCache.getIfPresent(sql);
        if (statement == null) {
            statement = this.connection.prepareStatement(sql);
            this.preparedStatementCache.put(sql, statement);
        }
        statement.clearParameters();
        statement.clearBatch();
        return statement;
    }

    private String placeholders(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append("?");
        }
        return builder.toString();
    }

    private String buildInsertSql(String tableName, List<String> columns) {
        if (columns.isEmpty()) {
            return "insert into " + tableName + " default values";
        }
        return "insert into " + tableName + "(" + String.join(",", columns) + ") values (" + placeholders(columns.size()) + ")";
    }

    private int bindObjects(PreparedStatement statement, List<?> values, int startIndex) throws SQLException {
        int index = startIndex;
        for (Object value : values) {
            statement.setObject(index, value);
            index++;
        }
        return index;
    }

    private List<Map.Entry<String, Object>> getUpdateEntries(SqlData values) {
        List<Map.Entry<String, Object>> entries = new LinkedList<>();
        for (Map.Entry<String, Object> entry : values.getData().entrySet()) {
            if (!"id".equalsIgnoreCase(entry.getKey())) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private String getUpdateSetSql(List<Map.Entry<String, Object>> entries) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : entries) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(entry.getKey()).append(" = ?");
        }
        return builder.toString();
    }

    private int bindEntries(PreparedStatement statement, List<Map.Entry<String, Object>> entries, int startIndex) throws SQLException {
        int index = startIndex;
        for (Map.Entry<String, Object> entry : entries) {
            statement.setObject(index, entry.getValue());
            index++;
        }
        return index;
    }

    private String buildLimitClause(int start, int length) {
        if (length <= 0) {
            return "";
        }
        return " LIMIT " + Math.max(start, 0) + "," + length;
    }

    public synchronized void destroyed() {
        this.close();
    }

    /**
     * 数据库资源关闭和释放
     */
    public synchronized void close() {
        SQLException exception = null;
        if (null != statement) {
            try {
                statement.close();
            } catch (SQLException e) {
                exception = addCloseException(exception, e);
            } finally {
                statement = null;
            }
        }

        try {
            this.preparedStatementCache.invalidateAll();
            this.preparedStatementCache.cleanUp();
        } finally {
            if (null != connection) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    exception = addCloseException(exception, e);
                } finally {
                    connection = null;
                }
            }
        }

        if (exception != null) {
            logSqlException("Sqlite数据库关闭时异常 ", exception);
        }
    }

    private static Map<String, Field> getFieldMap(Class<?> clazz) {
        return FIELD_CACHE.get(clazz);
    }

    private static SQLException addCloseException(SQLException current, SQLException next) {
        if (current == null) {
            return next;
        }
        current.addSuppressed(next);
        return current;
    }

    private static void logSqlException(String message, SQLException e) {
        EasySQLX plugin = EasySQLX.getInstance();
        if (plugin != null) {
            plugin.getLogger().error(message, e);
        }
    }

    private static final class ResultSetMapper {
        private final String[] columns;
        private final Map<String, Field> fields;

        private ResultSetMapper(String[] columns, Map<String, Field> fields) {
            this.columns = columns;
            this.fields = fields;
        }

        private static ResultSetMapper of(ResultSet resultSet, Class<?> clazz) throws SQLException {
            ResultSetMetaData metadata = resultSet.getMetaData();
            String[] columns = new String[metadata.getColumnCount()];
            for (int i = 0; i < columns.length; i++) {
                columns[i] = metadata.getColumnName(i + 1);
            }
            return new ResultSetMapper(columns, getFieldMap(clazz));
        }
    }

}

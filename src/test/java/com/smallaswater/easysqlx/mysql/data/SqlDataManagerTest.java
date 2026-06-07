package com.smallaswater.easysqlx.mysql.data;

import com.smallaswater.easysqlx.common.data.SqlData;
import com.smallaswater.easysqlx.common.data.SqlDataList;
import com.smallaswater.easysqlx.exceptions.MySqlLoginException;
import com.smallaswater.easysqlx.mysql.BaseMySql;
import com.smallaswater.easysqlx.mysql.manager.UseTableSqlManager;
import com.smallaswater.easysqlx.mysql.utils.ChunkSqlType;
import com.smallaswater.easysqlx.mysql.utils.LoginPool;
import com.smallaswater.easysqlx.mysql.utils.UserData;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SqlDataManagerTest {

    @Test
    public void selectExecuteRunsQueryOnceAndBindsObjects() {
        DatabaseProbe probe = new DatabaseProbe(
                new String[]{"id", "name"},
                Arrays.asList(new Object[]{7, "Alice"}, new Object[]{8, "Bob"})
        );

        SqlDataList<SqlData> result = SqlDataManager.selectExecute(
                poolFor(probe),
                "SELECT id, name FROM users WHERE id = ?",
                new ChunkSqlType(1, 7)
        );

        assertEquals(2, result.size());
        assertEquals(7, result.get().getInt("id"));
        assertEquals("Alice", result.get().getString("name"));
        assertEquals(1, probe.executeQueryCount);
        assertEquals(2, probe.getColumnNameCount);
        assertEquals(Collections.singletonList(7), probe.objectBinds);
        assertTrue(probe.stringBinds.isEmpty());
        assertTrue(probe.resultSetClosed);
        assertTrue(probe.preparedStatementClosed);
        assertTrue(probe.connectionClosed);
    }

    @Test
    public void selectExecuteOverloadAcceptsNonEmptyColumnAndTable() {
        DatabaseProbe probe = new DatabaseProbe(
                new String[]{"id"},
                Collections.singletonList(new Object[]{7})
        );

        SqlDataList<SqlData> result = SqlDataManager.selectExecute(
                poolFor(probe),
                "id",
                "users",
                "id = ?",
                new ChunkSqlType(1, 7)
        );

        assertEquals(1, result.size());
        assertEquals(7, result.get().getInt("id"));
        assertEquals("SELECT id FROM users WHERE id = ?", probe.sql);
        assertEquals(Collections.singletonList(7), probe.objectBinds);
    }

    @Test
    public void selectExecuteBuildsLikeOrderByAndLimitInMysqlOrder() {
        DatabaseProbe probe = new DatabaseProbe(new String[0], Collections.emptyList());

        SqlDataManager.selectExecute(
                poolFor(probe),
                "id",
                "users",
                "name",
                "?",
                0,
                10,
                "id",
                "id DESC",
                "COUNT(*) > 0",
                new ChunkSqlType(1, "A%")
        );

        assertEquals(
                "SELECT id FROM users WHERE name LIKE ? GROUP BY id HAVING COUNT(*) > 0 ORDER BY id DESC LIMIT 0,10",
                probe.sql
        );
        assertEquals(Collections.singletonList("A%"), probe.objectBinds);
    }

    @Test
    public void executeSqlBindsObjects() {
        DatabaseProbe probe = new DatabaseProbe(new String[0], Collections.emptyList());

        boolean success = SqlDataManager.executeSql(
                poolFor(probe),
                "UPDATE users SET score = ? WHERE id = ?",
                new ChunkSqlType(1, 99),
                new ChunkSqlType(2, 7)
        );

        assertTrue(success);
        assertEquals(1, probe.executeCount);
        assertEquals(Arrays.asList(99, 7), probe.objectBinds);
        assertTrue(probe.stringBinds.isEmpty());
    }

    @Test
    public void isExistsUsesLimitOneQuery() {
        DatabaseProbe probe = new DatabaseProbe(
                new String[]{"1"},
                Collections.singletonList(new Object[]{1})
        );

        assertTrue(SqlDataManager.isExists(poolFor(probe), "users", "id", "7"));

        assertEquals("SELECT 1 FROM users WHERE id = ? LIMIT 1", probe.sql);
        assertEquals(1, probe.executeQueryCount);
    }

    @Test
    public void isTableColumnDataReturnsTrueWhenTableExists() {
        DatabaseProbe probe = new DatabaseProbe(
                new String[]{"1"},
                Collections.singletonList(new Object[]{1})
        );

        assertTrue(SqlDataManager.isTableColumnData(poolFor(probe), "test_db", "users"));

        assertEquals("SELECT 1 FROM information_schema.TABLES WHERE table_schema = ? AND table_name = ? LIMIT 1", probe.sql);
        assertEquals(Arrays.asList("test_db", "users"), probe.objectBinds);
    }

    @Test
    public void insertDataBatchUsesJdbcBatchAndBindsObjects() {
        DatabaseProbe probe = new DatabaseProbe(new String[0], Collections.emptyList());
        LinkedList<SqlData> rows = new LinkedList<>();
        rows.add(new SqlData().put("id", 1).put("name", "Alice"));
        rows.add(new SqlData().put("id", 2).put("name", "Bob"));

        assertTrue(SqlDataManager.insertData(poolFor(probe), "users", rows));

        assertEquals("INSERT INTO users (id,name) VALUES (?,?)", probe.sql);
        assertEquals(2, probe.addBatchCount);
        assertEquals(1, probe.executeBatchCount);
        assertEquals(Arrays.asList(1, "Alice", 2, "Bob"), probe.objectBinds);
        assertTrue(probe.stringBinds.isEmpty());
        assertEquals(1, probe.commitCount);
        assertEquals(0, probe.rollbackCount);
    }

    @Test
    public void insertDataBatchFlushesLargeInputInChunks() {
        DatabaseProbe probe = new DatabaseProbe(new String[0], Collections.emptyList());
        LinkedList<SqlData> rows = new LinkedList<>();
        for (int i = 0; i < 501; i++) {
            rows.add(new SqlData().put("id", i).put("name", "User" + i));
        }

        assertTrue(SqlDataManager.insertData(poolFor(probe), "users", rows));

        assertEquals(501, probe.addBatchCount);
        assertEquals(2, probe.executeBatchCount);
        assertEquals(1, probe.commitCount);
        assertEquals(0, probe.rollbackCount);
    }

    @Test
    public void insertDataWithEmptySqlDataUsesDefaultValuesSql() {
        DatabaseProbe probe = new DatabaseProbe(new String[0], Collections.emptyList());

        assertTrue(SqlDataManager.insertData(poolFor(probe), "users", new SqlData()));

        assertEquals("INSERT INTO users () VALUES ()", probe.sql);
        assertEquals(1, probe.executeCount);
        assertTrue(probe.objectBinds.isEmpty());
    }

    @Test
    public void insertDataKeepsBasicValuesAndStringifiesCustomValues() {
        DatabaseProbe probe = new DatabaseProbe(new String[0], Collections.emptyList());
        SqlData data = new SqlData()
                .put("id", 7)
                .put("role", Role.ADMIN)
                .put("profile", new CustomValue("Steve"));

        assertTrue(SqlDataManager.insertData(poolFor(probe), "users", data));

        assertEquals(Arrays.asList(7, "ADMIN", "custom:Steve"), probe.objectBinds);
    }

    @Test
    public void insertDataBatchStringifiesCustomValues() {
        DatabaseProbe probe = new DatabaseProbe(new String[0], Collections.emptyList());
        LinkedList<SqlData> rows = new LinkedList<>();
        rows.add(new SqlData().put("id", 1).put("profile", new CustomValue("Alice")));
        rows.add(new SqlData().put("id", 2).put("profile", new CustomValue("Bob")));

        assertTrue(SqlDataManager.insertData(poolFor(probe), "users", rows));

        assertEquals(Arrays.asList(1, "custom:Alice", 2, "custom:Bob"), probe.objectBinds);
    }

    @Test
    public void setDataStringifiesCustomValues() {
        DatabaseProbe probe = new DatabaseProbe(new String[0], Collections.emptyList());

        assertTrue(SqlDataManager.setData(
                poolFor(probe),
                "users",
                new SqlData().put("profile", new CustomValue("Steve")),
                new SqlData().put("id", 7)
        ));

        assertEquals(Arrays.asList("custom:Steve", 7), probe.objectBinds);
    }

    @Test
    public void deleteColumnBuildsDropColumnSqlWithoutBindingIdentifier() throws Exception {
        TestMySql mysql = new TestMySql();

        assertTrue(mysql.deleteColumn("users", "score"));

        assertEquals("ALTER TABLE `users` DROP `score`", mysql.lastSql);
        assertEquals(0, mysql.lastTypes.length);
    }

    @Test
    public void useTableDeleteColumnKeepsConfiguredTableNameAsTableArgument() throws Exception {
        RecordingUseTableSqlManager manager = recordingUseTableManager("users");

        assertTrue(manager.deleteColumn("score"));

        assertEquals("ALTER TABLE `users` DROP `score`", manager.lastSql);
        assertEquals(0, manager.lastTypes.length);
    }

    @Test
    public void defaultMysqlParametersEnablePreparedStatementCache() throws Exception {
        BaseMySql mysql = new TestMySql();

        Field field = BaseMySql.class.getDeclaredField("connectionParameters");
        field.setAccessible(true);
        String parameters = (String) field.get(mysql);

        assertTrue(parameters.contains("cachePrepStmts=true"));
        assertTrue(parameters.contains("prepStmtCacheSize=250"));
        assertTrue(parameters.contains("prepStmtCacheSqlLimit=2048"));
        assertTrue(parameters.contains("useServerPrepStmts=true"));
        assertTrue(parameters.contains("rewriteBatchedStatements=true"));
    }

    @Test
    public void getAllDataSupportsLimitClauseForLargeTables() {
        TestMySql mysql = new TestMySql();

        mysql.getAllData("users", "id,name", 10, 20);

        assertEquals("SELECT id,name FROM `users` LIMIT 10,20", mysql.lastQuerySql);
    }

    @Test
    public void getDataSupportsLimitClauseWithWhereParameters() {
        TestMySql mysql = new TestMySql();

        mysql.getData("users", "id", new SqlData().put("name", "Alice"), 0, 10);

        assertEquals("SELECT id FROM `users` WHERE name=? LIMIT 0,10", mysql.lastQuerySql);
        assertEquals(1, mysql.lastQueryTypes.length);
        assertEquals("Alice", mysql.lastQueryTypes[0].getObjectValue());
    }

    @Test
    public void getDataSizeStringifiesCustomValues() {
        DatabaseProbe probe = new DatabaseProbe(
                new String[]{"count"},
                Collections.singletonList(new Object[]{3})
        );
        BaseMySql mysql = new DataSizeMySql(probe);

        int size = mysql.getDataSize(
                "WHERE profile = ? AND id = ?",
                "users",
                new ChunkSqlType(1, new CustomValue("Steve")),
                new ChunkSqlType(2, 7)
        );

        assertEquals(3, size);
        assertEquals("SELECT COUNT(*) FROM `users` WHERE profile = ? AND id = ?", probe.sql);
        assertEquals(Arrays.asList("custom:Steve", 7), probe.objectBinds);
    }

    @Test
    public void tableAndColumnExistenceReturnFalseWhenConnectionUnavailable() {
        BaseMySql mysql = new NullConnectionMySql();

        assertFalse(mysql.isExistTable("users"));
        assertFalse(mysql.isExistColumn("users", "score"));
    }

    @Test
    public void setDataRejectsEmptyUpdateDataWithoutExecutingSql() {
        DatabaseProbe probe = new DatabaseProbe(new String[0], Collections.emptyList());

        assertFalse(SqlDataManager.setData(
                poolFor(probe),
                "users",
                new SqlData(),
                new SqlData().put("id", 1)
        ));

        assertNull(probe.sql);
    }

    @Test
    public void setDataRejectsEmptyWhereWithoutExecutingSql() {
        DatabaseProbe probe = new DatabaseProbe(new String[0], Collections.emptyList());

        assertFalse(SqlDataManager.setData(
                poolFor(probe),
                "users",
                new SqlData().put("name", "Alice"),
                new SqlData()
        ));

        assertNull(probe.sql);
    }

    private static LoginPool poolFor(DatabaseProbe probe) {
        LoginPool pool = new LoginPool("127.0.0.1", "root", "test");
        pool.dataSource = new ProbeDataSource(probe);
        return pool;
    }

    private static RecordingUseTableSqlManager recordingUseTableManager(String tableName) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        RecordingUseTableSqlManager manager = (RecordingUseTableSqlManager) unsafe.allocateInstance(RecordingUseTableSqlManager.class);
        manager.init(tableName);
        return manager;
    }

    private static Object proxy(Class<?> type, InvocationHandler handler) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, handler);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class TestMySql extends BaseMySql {
        private String lastSql;
        private ChunkSqlType[] lastTypes;
        private String lastQuerySql;
        private ChunkSqlType[] lastQueryTypes;

        private TestMySql() {
            super(null, new UserData("root", "password", "127.0.0.1", 3306, "test"));
        }

        @Override
        public boolean executeSql(String sql, ChunkSqlType... value) {
            this.lastSql = sql;
            this.lastTypes = value;
            return true;
        }

        @Override
        public SqlDataList<SqlData> getData(String sql, ChunkSqlType... types) {
            this.lastQuerySql = sql;
            this.lastQueryTypes = types;
            return new SqlDataList<>(sql, types);
        }
    }

    private static final class DataSizeMySql extends BaseMySql {
        private final DatabaseProbe probe;

        private DataSizeMySql(DatabaseProbe probe) {
            super(null, new UserData("root", "password", "127.0.0.1", 3306, "test"));
            this.probe = probe;
        }

        @Override
        public Connection getConnection() {
            return probe.connection();
        }
    }

    private static final class NullConnectionMySql extends BaseMySql {
        private NullConnectionMySql() {
            super(null, new UserData("root", "password", "127.0.0.1", 3306, "test"));
        }

        @Override
        public Connection getConnection() {
            return null;
        }
    }

    private static final class RecordingUseTableSqlManager extends UseTableSqlManager {
        private String lastSql;
        private ChunkSqlType[] lastTypes;

        private RecordingUseTableSqlManager() throws MySqlLoginException {
            super(null, new UserData("root", "password", "127.0.0.1", 3306, "test"), "users");
        }

        private void init(String tableName) {
            this.tableName = tableName;
        }

        @Override
        public boolean executeSql(String sql, ChunkSqlType... value) {
            this.lastSql = sql;
            this.lastTypes = value;
            return true;
        }
    }

    private static final class ProbeDataSource extends HikariDataSource {
        private final DatabaseProbe probe;

        private ProbeDataSource(DatabaseProbe probe) {
            this.probe = probe;
        }

        @Override
        public Connection getConnection() {
            return probe.connection();
        }
    }

    private static final class DatabaseProbe {
        private final String[] columns;
        private final List<Object[]> rows;
        private final List<Object> objectBinds = new ArrayList<>();
        private final List<Object> stringBinds = new ArrayList<>();
        private String sql;
        private int executeQueryCount;
        private int executeCount;
        private int addBatchCount;
        private int executeBatchCount;
        private int getColumnNameCount;
        private int commitCount;
        private int rollbackCount;
        private boolean autoCommit = true;
        private boolean connectionClosed;
        private boolean preparedStatementClosed;
        private boolean resultSetClosed;

        private DatabaseProbe(String[] columns, List<Object[]> rows) {
            this.columns = columns;
            this.rows = rows;
        }

        private Connection connection() {
            return (Connection) proxy(Connection.class, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();
                    if ("prepareStatement".equals(name)) {
                        sql = (String) args[0];
                        return preparedStatement();
                    }
                    if ("getAutoCommit".equals(name)) {
                        return autoCommit;
                    }
                    if ("setAutoCommit".equals(name)) {
                        autoCommit = (Boolean) args[0];
                        return null;
                    }
                    if ("commit".equals(name)) {
                        commitCount++;
                        return null;
                    }
                    if ("rollback".equals(name)) {
                        rollbackCount++;
                        return null;
                    }
                    if ("close".equals(name)) {
                        connectionClosed = true;
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
            });
        }

        private PreparedStatement preparedStatement() {
            return (PreparedStatement) proxy(PreparedStatement.class, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();
                    if ("setObject".equals(name)) {
                        objectBinds.add(args[1]);
                        return null;
                    }
                    if ("setString".equals(name)) {
                        stringBinds.add(args[1]);
                        return null;
                    }
                    if ("executeQuery".equals(name)) {
                        executeQueryCount++;
                        return resultSet();
                    }
                    if ("execute".equals(name)) {
                        executeCount++;
                        return true;
                    }
                    if ("addBatch".equals(name)) {
                        addBatchCount++;
                        return null;
                    }
                    if ("executeBatch".equals(name)) {
                        executeBatchCount++;
                        return new int[addBatchCount];
                    }
                    if ("getMetaData".equals(name)) {
                        return resultSetMetaData();
                    }
                    if ("close".equals(name)) {
                        preparedStatementClosed = true;
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
            });
        }

        private ResultSet resultSet() {
            return (ResultSet) proxy(ResultSet.class, new InvocationHandler() {
                private int cursor = -1;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();
                    if ("next".equals(name)) {
                        cursor++;
                        return cursor < rows.size();
                    }
                    if ("getObject".equals(name)) {
                        return rows.get(cursor)[(Integer) args[0] - 1];
                    }
                    if ("getInt".equals(name)) {
                        Object value = rows.get(cursor)[(Integer) args[0] - 1];
                        return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(String.valueOf(value));
                    }
                    if ("getMetaData".equals(name)) {
                        return resultSetMetaData();
                    }
                    if ("close".equals(name)) {
                        resultSetClosed = true;
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
            });
        }

        private ResultSetMetaData resultSetMetaData() {
            return (ResultSetMetaData) proxy(ResultSetMetaData.class, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();
                    if ("getColumnCount".equals(name)) {
                        return columns.length;
                    }
                    if ("getColumnName".equals(name)) {
                        getColumnNameCount++;
                        return columns[(Integer) args[0] - 1];
                    }
                    return defaultValue(method.getReturnType());
                }
            });
        }
    }

    private enum Role {
        ADMIN
    }

    private static final class CustomValue {
        private final String value;

        private CustomValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "custom:" + value;
        }
    }
}

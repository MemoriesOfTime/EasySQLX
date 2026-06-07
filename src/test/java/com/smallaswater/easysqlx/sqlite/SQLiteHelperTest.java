package com.smallaswater.easysqlx.sqlite;

import com.smallaswater.easysqlx.common.data.SqlData;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SQLiteHelperTest {

    @Test
    public void publicDatabaseMethodsAreSynchronizedToProtectCachedStatements() throws Exception {
        assertTrue(Modifier.isSynchronized(SQLiteHelper.class.getMethod("exists", String.class).getModifiers()));
        assertTrue(Modifier.isSynchronized(SQLiteHelper.class.getMethod("add", String.class, LinkedList.class).getModifiers()));
        assertTrue(Modifier.isSynchronized(SQLiteHelper.class.getMethod("hasData", String.class, String.class, String.class).getModifiers()));
        assertTrue(Modifier.isSynchronized(SQLiteHelper.class.getMethod("close").getModifiers()));
    }

    @Test
    public void existsUsesSqliteMasterLimitOneAndClosesResultSet() throws Exception {
        DatabaseProbe probe = new DatabaseProbe();
        SQLiteHelper helper = helperWithConnectionProbe(probe);

        try {
            assertTrue(helper.exists("users"));

            assertEquals("SELECT 1 FROM sqlite_master WHERE type='table' AND name COLLATE NOCASE = ? LIMIT 1", probe.sql);
            assertEquals(Arrays.asList("users"), probe.objectBinds);
            assertTrue(probe.resultSetClosed);
        } finally {
            helper.close();
        }
    }

    @Test
    public void existsMatchesSqliteTableNamesCaseInsensitively() throws Exception {
        SQLiteHelper helper = newHelper();
        try {
            helper.addTable("users", new SQLiteHelper.DBTable("id", "integer"));

            assertTrue(helper.exists("USERS"));
            assertTrue(helper.exists("Users"));
            assertFalse(helper.exists("missing"));
        } finally {
            closeAndDelete(helper);
        }
    }

    @Test
    public void hasDataUsesLimitOneAndObjectBinding() throws Exception {
        DatabaseProbe probe = new DatabaseProbe();
        SQLiteHelper helper = helperWithConnectionProbe(probe);

        try {
            assertTrue(helper.hasData("users", "id", "7"));

            assertEquals("SELECT 1 FROM users WHERE id = ? LIMIT 1", probe.sql);
            assertEquals(Arrays.asList("7"), probe.objectBinds);
            assertTrue(probe.stringBinds.isEmpty());
            assertTrue(probe.resultSetClosed);
        } finally {
            helper.close();
        }
    }

    @Test
    public void addUsesPreparedStatementValues() throws Exception {
        SQLiteHelper helper = newHelper();
        try {
            helper.addTable("users", new SQLiteHelper.DBTable("id", "integer").put("name", "text"));

            helper.add("users", new SqlData().put("id", 1).put("name", "O'Reilly"));

            assertTrue(helper.hasData("users", "name", "O'Reilly"));
        } finally {
            closeAndDelete(helper);
        }
    }

    @Test
    public void addBatchInsertsRows() throws Exception {
        SQLiteHelper helper = newHelper();
        try {
            helper.addTable("users", new SQLiteHelper.DBTable("id", "integer").put("name", "text"));
            LinkedList<SqlData> rows = new LinkedList<>();
            rows.add(new SqlData().put("id", 1).put("name", "Alice"));
            rows.add(new SqlData().put("id", 2).put("name", "Bob"));

            helper.add("users", rows);

            assertTrue(helper.hasData("users", "name", "Alice"));
            assertTrue(helper.hasData("users", "name", "Bob"));
        } finally {
            closeAndDelete(helper);
        }
    }

    @Test
    public void addSupportsEmptySqlDataWithDefaultValues() throws Exception {
        SQLiteHelper helper = newHelper();
        try {
            helper.addTable("users", new SQLiteHelper.DBTable("id", "integer primary key autoincrement"));

            helper.add("users", new SqlData());
            LinkedList<SqlData> rows = new LinkedList<>();
            rows.add(new SqlData());
            rows.add(new SqlData());
            helper.add("users", rows);

            LinkedList<UserRow> result = helper.getAll("users", UserRow.class);
            assertEquals(3, result.size());
        } finally {
            closeAndDelete(helper);
        }
    }

    @Test
    public void getAllSupportsLimitClauseForLargeTables() throws Exception {
        SQLiteHelper helper = newHelper();
        try {
            helper.addTable("users", new SQLiteHelper.DBTable("id", "integer").put("name", "text"));
            helper.add("users", new SqlData().put("id", 1).put("name", "Alice"));
            helper.add("users", new SqlData().put("id", 2).put("name", "Bob"));

            LinkedList<UserRow> rows = helper.getAll("users", UserRow.class, 0, 1);

            assertEquals(1, rows.size());
            assertEquals(1, rows.getFirst().id);
            assertEquals("Alice", rows.getFirst().name);
        } finally {
            closeAndDelete(helper);
        }
    }

    @Test
    public void setWithOnlyIdDoesNotGenerateEmptyUpdate() throws Exception {
        SQLiteHelper helper = newHelper();
        try {
            helper.addTable("users", new SQLiteHelper.DBTable("id", "integer").put("name", "text"));
            helper.add("users", new SqlData().put("id", 1).put("name", "Alice"));

            helper.set("users", 1, new SqlData().put("id", 1));

            assertTrue(helper.hasData("users", "name", "Alice"));
        } finally {
            closeAndDelete(helper);
        }
    }

    @Test
    public void addBatchFlushesLargeInputInChunks() throws Exception {
        DatabaseProbe probe = new DatabaseProbe();
        SQLiteHelper helper = helperWithConnectionProbe(probe);

        try {
            LinkedList<SqlData> rows = new LinkedList<>();
            for (int i = 0; i < 501; i++) {
                rows.add(new SqlData().put("id", i).put("name", "User" + i));
            }

            helper.add("users", rows);

            assertEquals("insert into users(id,name) values (?,?)", probe.sql);
            assertEquals(501, probe.addBatchCount);
            assertEquals(2, probe.executeBatchCount);
            assertEquals(1, probe.commitCount);
            assertEquals(0, probe.rollbackCount);
        } finally {
            helper.close();
        }
    }

    @Test
    public void addBatchClearsCachedStatementBatchAfterColumnMismatchFailure() throws Exception {
        SQLiteHelper helper = newHelper();
        try {
            helper.addTable("users", new SQLiteHelper.DBTable("id", "integer").put("name", "text"));

            LinkedList<SqlData> invalidRows = new LinkedList<>();
            invalidRows.add(new SqlData().put("id", 1).put("name", "stale"));
            invalidRows.add(new SqlData().put("name", "bad").put("id", 2));

            try {
                helper.add("users", invalidRows);
                fail("列顺序不一致时批量插入应失败");
            } catch (RuntimeException expected) {
                assertTrue(expected.getCause() instanceof IllegalArgumentException);
            }

            LinkedList<SqlData> validRows = new LinkedList<>();
            validRows.add(new SqlData().put("id", 3).put("name", "fresh"));

            helper.add("users", validRows);

            assertFalse(helper.hasData("users", "name", "stale"));
            assertTrue(helper.hasData("users", "name", "fresh"));
        } finally {
            closeAndDelete(helper);
        }
    }

    @Test
    public void closeClosesCachedStatementsAndConnectionWhenStatementCloseFails() throws Exception {
        DatabaseProbe probe = new DatabaseProbe();
        probe.throwOnStatementClose = true;
        SQLiteHelper helper = helperWithConnectionProbe(probe);
        Field statementField = SQLiteHelper.class.getDeclaredField("statement");
        statementField.setAccessible(true);
        statementField.set(helper, probe.statement());

        helper.exists("users");
        helper.close();

        assertTrue(probe.statementClosed);
        assertTrue(probe.preparedStatementClosed);
        assertTrue(probe.connectionClosed);
    }

    private static SQLiteHelper helperWithConnectionProbe(DatabaseProbe probe) throws Exception {
        SQLiteHelper helper = newHelper();
        Field field = SQLiteHelper.class.getDeclaredField("connection");
        field.setAccessible(true);
        field.set(helper, probe.connection());
        return helper;
    }

    private static SQLiteHelper newHelper() throws Exception {
        File file = File.createTempFile("easysqlx-sqlite-test", ".db");
        if (!file.delete()) {
            throw new IllegalStateException("临时 SQLite 文件删除失败: " + file);
        }
        return new SQLiteHelper(file.getAbsolutePath());
    }

    private static void closeAndDelete(SQLiteHelper helper) {
        String path = helper.getDbFilePath();
        helper.close();
        new File(path).delete();
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
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class DatabaseProbe {
        private final List<Object> objectBinds = new ArrayList<>();
        private final List<Object> stringBinds = new ArrayList<>();
        private String sql;
        private boolean resultSetClosed;
        private int addBatchCount;
        private int executeBatchCount;
        private int commitCount;
        private int rollbackCount;
        private boolean autoCommit = true;
        private boolean statementClosed;
        private boolean preparedStatementClosed;
        private boolean connectionClosed;
        private boolean throwOnStatementClose;

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

        private Statement statement() {
            return (Statement) proxy(Statement.class, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("close".equals(method.getName())) {
                        statementClosed = true;
                        if (throwOnStatementClose) {
                            throw new SQLException("statement close failed");
                        }
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
                        return resultSet();
                    }
                    if ("addBatch".equals(name)) {
                        addBatchCount++;
                        return null;
                    }
                    if ("executeBatch".equals(name)) {
                        executeBatchCount++;
                        return new int[addBatchCount];
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
                private boolean first = true;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();
                    if ("next".equals(name)) {
                        boolean result = first;
                        first = false;
                        return result;
                    }
                    if ("getInt".equals(name)) {
                        return 1;
                    }
                    if ("close".equals(name)) {
                        resultSetClosed = true;
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
            });
        }
    }

    public static class UserRow {
        public int id;
        public String name;
    }
}

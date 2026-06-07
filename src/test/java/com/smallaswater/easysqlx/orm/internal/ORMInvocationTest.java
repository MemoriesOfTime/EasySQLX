package com.smallaswater.easysqlx.orm.internal;

import com.smallaswater.easysqlx.common.data.SqlData;
import com.smallaswater.easysqlx.exceptions.MySqlLoginException;
import com.smallaswater.easysqlx.mysql.manager.SqlManager;
import com.smallaswater.easysqlx.mysql.utils.ChunkSqlType;
import com.smallaswater.easysqlx.mysql.utils.UserData;
import com.smallaswater.easysqlx.orm.annotations.dao.DoInsert;
import com.smallaswater.easysqlx.orm.annotations.entity.Column;
import com.smallaswater.easysqlx.orm.api.IDAO;
import com.smallaswater.easysqlx.orm.utils.Kind;
import com.smallaswater.easysqlx.orm.utils.Option;
import org.junit.Test;
import sun.misc.Unsafe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ORMInvocationTest {

    @Test
    public void doInsertCreatesTableOnlyOnce() throws Exception {
        RecordingSqlManager manager = recordingManager();
        ORMInvocation<ExampleDAO> invocation = new ORMInvocation<>(ExampleDAO.class, "users", manager);
        Method insert = ExampleDAO.class.getMethod("insert", ExampleEntity.class);

        invocation.invoke(null, insert, new Object[]{new ExampleEntity("Alice")});
        invocation.invoke(null, insert, new Object[]{new ExampleEntity("Bob")});

        assertEquals(1, manager.executeSqlStatements.size());
        assertTrue(manager.executeSqlStatements.get(0).startsWith("create table if not exists users("));
        assertEquals(2, manager.insertCalls);
    }

    @Test
    public void generatedDefaultClassIsCachedPerDaoInterface() throws Exception {
        RecordingSqlManager manager = recordingManager();
        ORMInvocation<ExampleDAO> first = new ORMInvocation<>(ExampleDAO.class, "users", manager);
        ORMInvocation<ExampleDAO> second = new ORMInvocation<>(ExampleDAO.class, "users", manager);

        Object firstDefaultObject = getDefaultDoProxyObject(first);
        Object secondDefaultObject = getDefaultDoProxyObject(second);

        assertSame(firstDefaultObject.getClass(), secondDefaultObject.getClass());
    }

    @Test
    public void generatedDefaultClassAvoidsJavassistClassOverloadForJava8Compatibility() throws Exception {
        assertFalse(
                "ORMInvocation must not call CtClass.toClass(Class), which requires Java 9 module APIs at runtime",
                hasMethodRef(
                        ORMInvocation.class,
                        "javassist/CtClass",
                        "toClass",
                        "(Ljava/lang/Class;)Ljava/lang/Class;"
                )
        );
    }

    @Test
    public void doInsertRejectsMethodsWithWrongParameterType() throws Exception {
        RecordingSqlManager manager = recordingManager();
        ORMInvocation<WrongParameterDAO> invocation = new ORMInvocation<>(WrongParameterDAO.class, "users", manager);
        Method insert = WrongParameterDAO.class.getMethod("insert", String.class);

        try {
            invocation.invoke(null, insert, new Object[]{"Alice"});
            fail("@DoInsert 参数类型错误时应直接拒绝");
        } catch (RuntimeException expected) {
            assertEquals("@DoInsert 修饰的方法只能有一个参数并且为范型类型!", expected.getMessage());
        }
        assertEquals(0, manager.executeSqlStatements.size());
        assertEquals(0, manager.insertCalls);
    }

    @Test
    public void uniqueOptionKeepsDefaultNotNullConstraint() throws Exception {
        RecordingSqlManager manager = recordingManager();
        ORMInvocation<UniqueOptionDAO> invocation = new ORMInvocation<>(UniqueOptionDAO.class, "users", manager);
        Method insert = UniqueOptionDAO.class.getMethod("insert", UniqueOptionEntity.class);

        invocation.invoke(null, insert, new Object[]{new UniqueOptionEntity("Alice")});

        String createSql = manager.executeSqlStatements.get(0);
        assertTrue(createSql.contains("name varchar(255) not null unique"));
        assertFalse(createSql.contains("name varchar(255) null unique"));
    }

    private static RecordingSqlManager recordingManager() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        RecordingSqlManager manager = (RecordingSqlManager) unsafe.allocateInstance(RecordingSqlManager.class);
        manager.init();
        return manager;
    }

    private static Object getDefaultDoProxyObject(ORMInvocation<?> invocation) throws Exception {
        Field field = ORMInvocation.class.getDeclaredField("defaultDoProxyObj");
        field.setAccessible(true);
        return field.get(invocation);
    }

    private static boolean hasMethodRef(Class<?> clazz, String owner, String name, String descriptor) throws Exception {
        byte[] classBytes = readClassBytes(clazz);
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(classBytes));
        if (input.readInt() != 0xCAFEBABE) {
            throw new IllegalArgumentException("不是有效的 class 文件: " + clazz.getName());
        }
        input.readUnsignedShort();
        input.readUnsignedShort();
        int constantPoolCount = input.readUnsignedShort();
        int[] tags = new int[constantPoolCount];
        Object[] constantPool = new Object[constantPoolCount];

        for (int i = 1; i < constantPoolCount; i++) {
            int tag = input.readUnsignedByte();
            tags[i] = tag;
            switch (tag) {
                case 1:
                    constantPool[i] = input.readUTF();
                    break;
                case 3:
                case 4:
                    input.skipBytes(4);
                    break;
                case 5:
                case 6:
                    input.skipBytes(8);
                    i++;
                    break;
                case 7:
                case 8:
                case 16:
                    constantPool[i] = input.readUnsignedShort();
                    break;
                case 9:
                case 10:
                case 11:
                case 12:
                case 18:
                    constantPool[i] = new int[]{input.readUnsignedShort(), input.readUnsignedShort()};
                    break;
                case 15:
                    input.readUnsignedByte();
                    input.readUnsignedShort();
                    break;
                default:
                    throw new IllegalArgumentException("不支持的 constant pool tag: " + tag);
            }
        }

        for (int i = 1; i < constantPoolCount; i++) {
            if (tags[i] != 10) {
                continue;
            }
            int[] methodRef = (int[]) constantPool[i];
            String className = (String) constantPool[(Integer) constantPool[methodRef[0]]];
            int[] nameAndType = (int[]) constantPool[methodRef[1]];
            String methodName = (String) constantPool[nameAndType[0]];
            String methodDescriptor = (String) constantPool[nameAndType[1]];
            if (owner.equals(className) && name.equals(methodName) && descriptor.equals(methodDescriptor)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] readClassBytes(Class<?> clazz) throws Exception {
        String resource = clazz.getName().replace('.', '/') + ".class";
        InputStream input = clazz.getClassLoader().getResourceAsStream(resource);
        if (input == null) {
            throw new IllegalArgumentException("找不到 class 资源: " + resource);
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    public interface ExampleDAO extends IDAO<ExampleEntity> {
        @DoInsert
        void insert(ExampleEntity entity);
    }

    public interface WrongParameterDAO extends IDAO<ExampleEntity> {
        @DoInsert
        void insert(String name);
    }

    public interface UniqueOptionDAO extends IDAO<UniqueOptionEntity> {
        @DoInsert
        void insert(UniqueOptionEntity entity);
    }

    public static class ExampleEntity {
        @Column(name = "id", kind = Kind.ID)
        public int id;

        @Column(name = "name", kind = Kind.VARCHAR)
        public String name;

        public ExampleEntity(String name) {
            this.name = name;
        }
    }

    public static class UniqueOptionEntity {
        @Column(name = "id", kind = Kind.ID)
        public int id;

        @Column(name = "name", kind = Kind.VARCHAR, options = {Option.UNIQUE})
        public String name;

        public UniqueOptionEntity(String name) {
            this.name = name;
        }
    }

    private static class RecordingSqlManager extends SqlManager {
        private List<String> executeSqlStatements;
        private int insertCalls;

        private RecordingSqlManager() throws MySqlLoginException {
            super(null, new UserData("root", "password", "127.0.0.1", 3306, "test"));
        }

        private void init() {
            this.executeSqlStatements = new ArrayList<>();
        }

        @Override
        public boolean executeSql(String sql, ChunkSqlType... value) {
            executeSqlStatements.add(sql);
            return true;
        }

        @Override
        public boolean insertData(String tableName, SqlData data) {
            insertCalls++;
            return true;
        }
    }
}

package com.smallaswater.easysql.orm.internal;

import com.smallaswater.easysql.mysql.data.SqlData;
import com.smallaswater.easysql.mysql.data.SqlDataList;
import com.smallaswater.easysql.mysql.utils.ChunkSqlType;
import com.smallaswater.easysql.orm.annotations.dao.*;
import com.smallaswater.easysql.orm.annotations.entity.*;
import com.smallaswater.easysql.orm.api.IDAO;
import com.smallaswater.easysql.orm.utils.Options;
import com.smallaswater.easysql.v3.mysql.manager.SqlManager;
import javassist.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ORMInvocation<T extends IDAO<?>> implements InvocationHandler {

    private final SqlManager manager;
    private final String table;
    private final Class<T> clazz; // 被代理的接口 class
    private final Class<?> entityClass;

    private Object defaultDoProxyObj;

    // 储存 name -> Field 的 map
    private final Map<String, Field> columnFields = new HashMap<>();

    public ORMInvocation(Class<T> clazz, String table, SqlManager manager) {
        this.manager = manager;
        this.table = table;
        this.clazz = clazz;
        this.entityClass = ((Class<?>) ((ParameterizedType) Arrays.stream(clazz.getGenericInterfaces())
                .filter(t -> t.getTypeName().replaceAll("<\\S+>", "").endsWith("IDAO"))
                .findFirst().get()).getActualTypeArguments()[0]);
        Class<?> defaultDoClass = generateDoDefaultClass();
        try {

            this.defaultDoProxyObj = defaultDoClass.newInstance();

            // 依赖注入
            Field m = defaultDoClass.getDeclaredField("manager");
            m.setAccessible(true);

            m.set(this.defaultDoProxyObj, this.manager);

            Field t = defaultDoClass.getDeclaredField("table");
            t.setAccessible(true);
            t.set(this.defaultDoProxyObj, this.table);

        } catch (Exception e) {
            e.printStackTrace();
        }
        Arrays.stream(entityClass.getDeclaredFields())
                .filter( t -> t.isAnnotationPresent(Column.class))
                .forEach( t -> {
                    Column c = t.getAnnotation(Column.class);
                    columnFields.put(c.name(), t);
                });
    }

    // 使用 javassist 生成执行 Default 的类(继承代理的接口类)
    private Class<?> generateDoDefaultClass() {
        ClassPool classPool = ClassPool.getDefault();

        classPool.insertClassPath(new ClassClassPath(this.getClass()));

        String cn = this.clazz.getName() + "ImplClass";
        CtClass clazz = classPool.makeClass(cn);
        CtClass interf  = null;

        try {
            interf = classPool.getCtClass(this.clazz.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        clazz.setInterfaces(new CtClass[]{interf});

        try {
            clazz.addField(CtField.make("public com.smallaswater.easysql.v3.mysql.manager.SqlManager manager;", clazz));
            clazz.addField(CtField.make("public java.lang.String table;", clazz));

            // 构造被拦截的方法，模拟拦截
            clazz.addMethod(CtMethod.make("public com.smallaswater.easysql.v3.mysql.manager.SqlManager getManager(){return manager;}", clazz));
            clazz.addMethod(CtMethod.make("public java.lang.String getTable(){return table;}", clazz));
            clazz.addMethod(CtMethod.make("public java.lang.String toString(){return \"Table: \" + table + \" Manager: \" + manager;}", clazz));

        } catch (Exception e) {
            e.printStackTrace();
        }


        Class<?> ret = null;
        try {

            ret = clazz.toClass(this.getClass().getClassLoader());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private Field findField(String identifier) {
        return this.columnFields.get(identifier);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        // 返回两个默认的方法
        if (method.getName().equals("getManager")) {
            return this.manager;
        }

        if (method.getName().equals("getTable")) {
            return this.table;
        }

        if (method.isAnnotationPresent(DoDefault.class)) {
            Object ret = null;
            try {
                method.setAccessible(true);
                ret = method.invoke(this.defaultDoProxyObj, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ret;
        }

        return handleMethod(method, args);
    }

    private Object handleMethod(Method method, Object[] args) {
        method.setAccessible(true);

        if (method.isAnnotationPresent(DoInsert.class)) {
            if (method.getParameterCount() != 1 && !method.getParameterTypes()[0].getName().equals(this.entityClass.getName())) {
                throw new RuntimeException("@DoInsert 修饰的方法只能有一个参数并且为范型类型!");
            }
            handleInsert(args);
            return null;
        }else if (method.isAnnotationPresent(DoExecute.class)) {
            if (!method.getGenericReturnType().getTypeName().equals("void")) {
                throw new RuntimeException("@DoExecute 修饰的方法不能有返回值!");
            }
            handleExecute(method.getAnnotation(DoExecute.class).value(), args);
            return null; // Execute 不支持返回
        }else if (method.isAnnotationPresent(DoQuery.class)) {
            return handleQuery(method.getAnnotation(DoQuery.class).value(), args);
        }else if (method.isAnnotationPresent(DoQueryRow.class)) {
            return handleQueryRow(method.getAnnotation(DoQueryRow.class).value(), args);
        }
        return null;
    }

    private Object handleQueryRow(String sql, Object[] args) {
        sql = sql.replace("{table}", this.table);
        if (checkParams(sql, args)) {
            throw new RuntimeException("SQL语法错误，入参数量不匹配");
        }
        ArrayList<ChunkSqlType> chunks = getSqlChunks(args);
        try {
            Object obj = this.entityClass.newInstance();
            SqlData data = this.manager.getData(sql, chunks.toArray(new ChunkSqlType[0])).get(0);
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                Object value = entry.getValue();
                String columnName = entry.getKey();
                Field columnFiled = findField(columnName);
                if (columnFiled != null) {
                    columnFiled.set(obj, value);
                }
            }
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    private List<Object> handleQuery(String sql, Object[] args) {
        sql = sql.replace("{table}", this.table);
        if (checkParams(sql, args)) {
            throw new RuntimeException("SQL语法错误，入参数量不匹配");
        }

        ArrayList<ChunkSqlType> chunks = getSqlChunks(args);

        List<Object> ret = new ArrayList<>();
        SqlDataList<SqlData> queryData = this.manager.getData(sql, chunks.toArray(new ChunkSqlType[0]));
        for (SqlData data : queryData) {
            try {
                Object obj = this.entityClass.newInstance();

                for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                    String columnName = entry.getKey();
                    Object value = entry.getValue();
                    Field columnFiled = findField(columnName);
                    if (columnFiled != null) {
                        columnFiled.set(obj, value);
                    }
                }

                ret.add(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }


    private void handleInsert(Object[] args) {
        Object entity = args[0];
        StringBuilder sb = new StringBuilder();
        Map<String, List<String>> uniqueConstraints = new LinkedHashMap<>(); // 联合唯一键
        AtomicReference<String> pkConstraintName = new AtomicReference<>(""); // 联合主键
        List<String> pkConstraints = new ArrayList<>();
        Map<String, String> foreignKeys = new LinkedHashMap<>(); // 外键

        SqlData data = new SqlData();

        Arrays.stream(this.entityClass.getDeclaredFields())
                .filter(it -> it.isAnnotationPresent(Column.class))
                .forEach(it -> {
                    Column column = it.getAnnotation(Column.class);
                    String option = getOption(column.type().toString(), column.options());

                    if (it.isAnnotationPresent(Constraint.class)) {
                        Constraint constraint = it.getAnnotation(Constraint.class);
                        switch (constraint.type()) {
                            case UNIQUE:
                                uniqueConstraints.putIfAbsent(constraint.name(), new ArrayList<>());
                                uniqueConstraints.get(constraint.name()).add(column.name());
                                break;
                            case PRIMARY:
                                pkConstraintName.set(constraint.name());
                                pkConstraints.add(column.name());
                                break;
                        }
                    }

                    if (it.isAnnotationPresent(ForeignKey.class)) {
                        ForeignKey foreignKey = it.getAnnotation(ForeignKey.class);
                        foreignKeys.put(column.name(), foreignKey.tableName()+"("+foreignKey.columnName()+")");
                    }

                    sb.append(column.name()).append(" ").append(option).append(",");


                    try {
                        Object value = it.get(entity);
                        if (it.isAnnotationPresent(AutoUUIDGenerate.class)) {
                            value = UUID.randomUUID().toString();  // 自动生成 uuid
                        }
                        if (!option.contains("auto_increment") && value != null) { // 跳过自增键和空值
                            data.put(column.name(), value);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
        for (Map.Entry<String, List<String>> entry : uniqueConstraints.entrySet()) {
            sb.append("constraint").append(" ").append(entry.getKey())
                    .append(" ").append("unique").append("(")
                    .append(Arrays.toString(entry.getValue().toArray(new String[0]))
                            .replace("[", "").replace("]", ""))
                    .append("),");
        }

        if (!pkConstraintName.get().equals("")) {
            sb.append("constraint").append(" ").append(pkConstraintName.get())
                    .append(" ").append("primary key").append("(")
                    .append(Arrays.toString(pkConstraints.toArray(new String[0]))
                            .replace("[", "").replace("]", ""))
                    .append("),");
        }

        for (Map.Entry<String, String> entry : foreignKeys.entrySet()) {
            sb.append("foreign key").append("(").append(entry.getKey()).append(")").append(" ")
                    .append("references").append(" ").append(entry.getValue()).append(",");
        }

        String createSQL = "create table if not exists "+this.table+"("+sb.substring(0, sb.length()-1)+")ENGINE=InnoDB DEFAULT CHARSET=utf8;";
        this.manager.executeSql(createSQL);

        this.manager.insertData(this.table, data);
    }

    private void handleExecute(String sql, Object[] args) {
        sql = sql.replace("{table}", this.table);
        if (checkParams(sql, args)) {
            throw new RuntimeException("SQL语法错误，入参数量不匹配");
        }

        ArrayList<ChunkSqlType> chunks = getSqlChunks(args);

        this.manager.executeSql(sql, chunks.toArray(new ChunkSqlType[0]));
    }


    private boolean checkParams(String sql, Object[] args) {
        if (args == null) {
            return sql.contains("?");
        }
        int cnt = 0;
        while (sql.contains("?")) {
            sql = sql.replaceFirst("\\?", "");
            cnt ++;
        }
        return args.length != cnt;
    }

    private ArrayList<ChunkSqlType> getSqlChunks(Object[] args) {
        if (args == null){
            return new ArrayList<>();
        }
        ArrayList<ChunkSqlType> chunks = new ArrayList<>();
        for (int i = 1; i <= args.length; i++) {
            ChunkSqlType chunk = new ChunkSqlType(i, args[i - 1].toString());
            chunks.add(chunk);
        }
        return chunks;
    }

    private String getOption(String option, Options[] options) {
        StringBuilder optionBuilder = new StringBuilder(option);
        for (Options op : options) {
            switch (op) {
                case PRIMARY_KEY:
                    if (!optionBuilder.toString().contains("primary key")) {
                        optionBuilder.append(" primary key");
                    }
                case UNIQUE:
                    if (!optionBuilder.toString().contains("unique")) {
                        optionBuilder.append(" unique");
                    }
                case NULL:
                    optionBuilder = new StringBuilder(optionBuilder.toString().replace("not null", "null")); // 覆盖 Types 里的选项
                    if (!optionBuilder.toString().contains("null")) {
                        optionBuilder.append(" null");
                    }
                    break;
                case NOT_NULL:
                    optionBuilder = new StringBuilder(optionBuilder.toString().replace("null", "not null"));
                    if (!optionBuilder.toString().contains("not null")) {
                        optionBuilder.append(" not null");
                    }
                    break;
            }
        }
        option = optionBuilder.toString();
        return option;
    }

}

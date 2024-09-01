package com.smallaswater.easysqlx.common.data;


import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author SmallasWater
 */
public class SqlData {

    private final LinkedHashMap<String, Object> data = new LinkedHashMap<>();

    public SqlData() {
    }

    public SqlData(String column, Object object) {
        data.put(column.toLowerCase(), object);
    }

    public LinkedHashMap<String, Object> getData() {
        return data;
    }

    /**
     * 添加数据
     *
     * @param column 字段名
     * @param object 数据
     */
    public SqlData put(String column, Object object) {
        data.put(column.toLowerCase(), object);
        return this;
    }

    /**
     * 获取数据
     *
     * @param column 字段名
     * @param <T>    数据类型
     */
    public <T> Object get(String column, T defaultValue) {
        return data.getOrDefault(column.toLowerCase(), defaultValue);
    }

    public int getInt(String key) {
        return this.getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        return Integer.parseInt(this.get(key, defaultValue).toString());
    }

    /**
     * 这个似乎没啥用
     * */
    @Deprecated
    public boolean isInt(String key) {
        Object val = data.get(key);
        return val instanceof Integer;
    }


    public double getDouble(String key) {
        return this.getDouble(key, 0.0D);
    }

    public double getDouble(String key, double defaultValue) {
        return Double.parseDouble(this.get(key, defaultValue).toString());
    }

    public long getLong(String key) {
        return this.getLong(key, 0L);
    }

    public long getLong(String key, long defaultValue) {
        return Long.parseLong(this.get(key, defaultValue).toString());
    }

    /**
     * 这个似乎没啥用
     * */
    @Deprecated
    public boolean isDouble(String key) {
        Object val = data.get(key);
        return val instanceof Double;
    }

    public String getString(String key) {
        return this.getString(key, "");
    }

    public String getString(String key, String defaultValue) {
        Object result = this.get(key, defaultValue);
        return String.valueOf(result);
    }

    /**
     * 这个似乎没啥用
     * */
    @Deprecated
    public boolean isString(String key) {
        Object val = data.get(key);
        return val instanceof String;
    }

    public boolean getBoolean(String key) {
        return this.getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(this.get(key, defaultValue).toString());
    }

    /**
     * 这个似乎没啥用
     * */
    @Deprecated
    public boolean isBoolean(String key) {
        Object val = data.get(key);
        return val instanceof Boolean;
    }

    /**
     * 获取所有字段名
     *
     * @return 字段名
     */
    public List<String> getColumns() {
        return new LinkedList<>(data.keySet());
    }

    public String getColumn() {
        return getColumn(0);
    }

    public Object getValue() {
        return getValue(0);
    }

    public String getColumn(int index) {
        LinkedList<String> names = new LinkedList<>(data.keySet());
        if (names.size() > index) {
            return names.get(index);
        }
        return null;
    }


    /**
     * 根据索引位置获取返回的数据
     */
    public Object getValue(int index) {
        LinkedList<Object> names = new LinkedList<>(data.values());
        if (names.size() > index) {
            return names.get(index);
        }
        return null;
    }


    public List<Object> getObjects() {
        return new LinkedList<>(data.values());
    }

    /**
     * 将获取的所有字段转换为字符串
     */
    public String getColumnToString() {
        return getDefaultToString(getColumns(), false);
    }

    /**
     * 将获取的所有数值转换为字符串
     */
    public String getObjectToString() {
        return getDefaultToString(getObjects(), true);
    }

    private String getDefaultToString(List<?> objects, boolean isValue) {
        StringBuilder builder = new StringBuilder();
        for (Object s : objects) {
            if (isValue) {
                builder.append("'").append(s).append("'").append(",");
            } else {
                builder.append(s).append(",");
            }

        }
        String str = builder.toString();
        return str.substring(0, str.length() - 1);
    }

    public String toUpdateValue() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if ("id".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            builder.append(entry.getKey()).append(" = '").append(entry.getValue().toString()).append("',");
        }
        String str = builder.toString();
        return str.substring(0, str.length() - 1);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        for (String name : data.keySet()) {
            Object o = data.get(name);
            if (o == null) {
                o = "null";
            }
            builder.append("column: ").append(name).append(" value: ").append(o);
        }
        builder.append("]");
        return builder.toString();
    }

    public static <T> SqlData classToSqlData(T object) throws IllegalArgumentException {
        SqlData data = new SqlData();
        boolean hasId = false;
        for (Field field : object.getClass().getFields()) {
            try {
                if (field.getType() == int.class || field.getType() == long.class) {
                    if ("id".equalsIgnoreCase(field.getName())) {
                        hasId = true;
                        continue;
                    }
                    data.put(field.getName(), field.getInt(object));
                } else {
                    data.put(field.getName(), field.get(object));
                }
            } catch (Exception ignore) {
            }
        }
        if (!hasId) {
            throw new IllegalArgumentException("The class must have an id field");
        }
        return data;
    }

    public static <T> SqlData classToSqlDataAsId(T object) {
        SqlData data = new SqlData();
        for (Field field : object.getClass().getFields()) {
            try {
                data.put(field.getName(), field.getInt(object));
            } catch (Exception ignore) {
            }
        }
        return data;
    }
}

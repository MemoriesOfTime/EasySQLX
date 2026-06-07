package com.smallaswater.easysqlx.mysql.utils;

/**
 * 这里 防SQL 注入
 *
 * @author SmallasWater
 */
public class ChunkSqlType {

    private final int i;

    private final Object value;

    public ChunkSqlType(int i, String value) {
        this(i, (Object) value);
    }

    public ChunkSqlType(int i, Object value) {
        this.i = i;
        this.value = value;
    }

    public int getI() {
        return i;
    }

    public String getValue() {
        return value == null ? null : String.valueOf(value);
    }

    public Object getObjectValue() {
        return value;
    }

    public Object getSqlValue() {
        return normalizeSqlValue(value);
    }

    public static Object normalizeSqlValue(Object value) {
        if (value == null || value instanceof String || value instanceof Number
                || value instanceof Boolean || value instanceof Character
                || value instanceof byte[] || value instanceof java.sql.Date
                || value instanceof java.sql.Time || value instanceof java.sql.Timestamp) {
            return value;
        }
        return String.valueOf(value);
    }
}

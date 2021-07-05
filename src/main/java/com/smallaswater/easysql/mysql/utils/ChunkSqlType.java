package com.smallaswater.easysql.mysql.utils;

/**
 * 这里 防SQL 注入
 *
 * @author SmallasWater
 */
public class ChunkSqlType {

    private final int i;

    private final String value;

    public ChunkSqlType(int i, String value) {
        this.i = i;
        this.value = value;
    }

    public int getI() {
        return i;
    }

    public String getValue() {
        return value;
    }
}

package com.smallaswater.easysql.mysql.data;

import com.smallaswater.easysql.mysql.utils.ChunkSqlType;

import java.util.LinkedList;

/**
 * @author SmallasWater
 */
public class SqlDataList<T extends SqlData> extends LinkedList<T> {

    private final String sql;

    private final ChunkSqlType[] types;


    public SqlDataList(String sql, ChunkSqlType... types) {
        this.types = types;
        this.sql = sql;
    }

    /**
     * 获取同一字段全部数据
     */
    public LinkedList<Object> getValueList(String column) {
        LinkedList<Object> objects = new LinkedList<>();
        for (SqlData data : this) {
            objects.add(data.get(column.toLowerCase(), null));
        }
        return objects;
    }


    public SqlData get() {
        if (size() == 0) {
            return new SqlData("", "");
        }
        return getFirst();
    }


    public String getSql() {
        return replace(sql, types);
    }

    private String replace(String s, ChunkSqlType... args) {
        char[] list = s.toCharArray();
        int size = 0;
        StringBuilder stringBuffer = new StringBuilder();
        for (char c : list) {
            if (String.valueOf(c).contains("?")) {
                stringBuffer.append(args[size].getValue());
                ++size;
            } else {
                stringBuffer.append(c);
            }
        }

        return stringBuffer.toString();
    }

}

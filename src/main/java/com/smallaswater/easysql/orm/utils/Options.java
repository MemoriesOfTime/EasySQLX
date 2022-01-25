package com.smallaswater.easysql.orm.utils;

public enum Options {

    PRIMARY_KEY("primary key"), // 主键 (当Entity里有 Types.ID 的字段时不能添加)

    UNIQUE("unique"),  // 列级定义唯一

    NOT_NULL("not null"),

    NULL("null"); // 可以是 null（可以覆盖掉 Types 内的 not null）


    private final String sql;

    Options(String sql) {
        this.sql = sql;
    }

    @Override
    public String toString() {
        return sql;
    }
}

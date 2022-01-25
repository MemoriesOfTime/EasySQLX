package com.smallaswater.easysql.mysql.utils;


/**
 * 参数表.. (没有的话执行setSql)
 *
 * @author SmallasWater
 */

public enum Types {

    /**
     * 字符串
     */
    CHAR("char", 255, "not null"),
    /**
     * ID自增键
     */
    ID("int", 10, "auto_increment primary key"),
    /**
     * 长字符串
     */
    VARCHAR("varchar", 255, "not null"),

    /**
     * 整数
     */
    INT("int", 10, "not null"),

    /**
     * 小数
     */
    DOUBLE("double", 255, 2, "not null"),

    /**
     * 存放2进制
     */
    BLOB("blob", 255, "not null"),
    /**
     * 时间
     */
    DATE("date", 255, "not null"),
    DATETIME("datetime", "", "", "not null"),
    /**
     * 字符串
     */
    TEXT("text", 1, "not null");

    protected String sql;
    protected Object size;
    protected Object otherSize = "";
    protected Object value;


    Types(String sql, Object... value) {
        this.sql = sql;
        this.size = value[0];
        if (value.length > 2) {
            this.otherSize = value[1];
            this.value = value[2];
        } else {
            this.value = value[1];
        }

    }

    public Types setOtherSize(Object otherSize) {
        this.otherSize = otherSize;
        return this;
    }

    public Types setSize(Object size) {
        this.size = size;
        return this;
    }

    public Types setValue(Object value) {
        this.value = value;
        return this;
    }

    public Types setSql(String sql) {
        this.sql = sql;
        return this;
    }

    @Override
    public String toString() {
        if (!"".equalsIgnoreCase(size.toString())) {
            if (!"".equals(otherSize)) {
                return this.sql + "(" + size.toString() + "," + otherSize.toString() + ") " + value.toString();
            }
            return this.sql + "(" + size.toString() + ") " + value.toString();
        }
        return sql + " " + value.toString();
    }
}

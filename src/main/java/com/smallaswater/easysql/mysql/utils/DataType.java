package com.smallaswater.easysql.mysql.utils;

import com.smallaswater.easysql.mysql.utils.apiinfo.ApiInfo;

import java.util.Objects;

/**
 * @author LT_Name
 */
@ApiInfo("数据类型")
public class DataType {

    private static final DataType CHAR = new DataType("char", 255, "not null");
    /**
     * ID自增键
     */
    private static final DataType ID = new DataType("int", 10, "auto_increment primary key");
    /**
     * 长字符串
     */
    private static final DataType VARCHAR = new DataType("varchar", 255, "not null");
    /**
     * 整数
     */
    private static final DataType INT = new DataType("int", 10, "not null");

    /**
     * 小数
     */
    private static final DataType DOUBLE = new DataType("double", 255, 2, "not null");
    /**
     * 存放2进制
     */
    private static final DataType BLOB = new DataType("blob", 255, "not null");
    /**
     * 时间
     */
    private static final DataType DATE = new DataType("date", 255, "not null");
    private static final DataType DATETIME = new DataType("datetime", "", "", "not null");
    /**
     * 字符串
     */
    private static final DataType TEXT = new DataType("text", 1, "not null");

    /**
     * nk中的玩家uuid
     */
    private static final DataType UUID = CHAR.clone().setSize(36);

    public static DataType getCHAR() {
        return CHAR.clone();
    }

    public static DataType getID() {
        return ID.clone();
    }

    public static DataType getVARCHAR() {
        return VARCHAR.clone();
    }

    public static DataType getINT() {
        return INT.clone();
    }

    public static DataType getDOUBLE() {
        return DOUBLE.clone();
    }

    public static DataType getBLOB() {
        return BLOB.clone();
    }

    public static DataType getDATE() {
        return DATE.clone();
    }

    public static DataType getDATETIME() {
        return DATETIME.clone();
    }

    public static DataType getTEXT() {
        return TEXT.clone();
    }

    public static DataType getUUID() {
        return UUID.clone();
    }

    private String sql;
    private Object size;
    private Object otherSize = "";
    private Object value;

    public DataType(String sql, Object... value) {
        this.sql = sql;
        this.size = value[0];
        if (value.length > 2) {
            this.otherSize = value[1];
            this.value = value[2];
        } else {
            this.value = value[1];
        }

    }

    public DataType setOtherSize(Object otherSize) {
        this.otherSize = otherSize;
        return this;
    }

    public DataType setSize(Object size) {
        this.size = size;
        return this;
    }

    public DataType setValue(Object value) {
        this.value = value;
        return this;
    }

    public DataType setSql(String sql) {
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

    @Override
    public DataType clone() {
        try {
            return (DataType) super.clone();
        }catch (Exception e) {
            return new DataType(sql, size, otherSize, value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataType type = (DataType) o;
        return Objects.equals(sql, type.sql) &&
                Objects.equals(size, type.size) &&
                Objects.equals(otherSize, type.otherSize) &&
                Objects.equals(value, type.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sql, size, otherSize, value);
    }
}

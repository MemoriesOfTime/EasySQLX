package com.smallaswater.easysql.orm.utils;

import com.smallaswater.easysql.mysql.utils.DataType;

/**
 * 和 {@link com.smallaswater.easysql.mysql.utils.DataType} 兼容
 * Compat with {@link com.smallaswater.easysql.mysql.utils.DataType}
 */
public enum Kind {
    CHAR(DataType.getCHAR().toString()),

    ID(DataType.getID().toString()),

    VARCHAR(DataType.getVARCHAR().toString()),

    INT(DataType.getINT().toString()),

    DOUBLE(DataType.getDOUBLE().toString()),

    BLOB(DataType.getBLOB().toString()),

    DATE(DataType.getDATE().toString()),

    DATETIME(DataType.getDATETIME().toString()),

    TEXT(DataType.getTEXT().toString()),

    UUID(DataType.getUUID().toString());


    private final String type;

    Kind(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return this.type;
    }
}

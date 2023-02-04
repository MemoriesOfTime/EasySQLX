package com.smallaswater.easysql.mysql.utils;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * 创建数据表传入的参数.
 * name 为自定义名称
 * type Mysql创建表参数
 *
 * @author SmallasWater
 */
public class TableType {

    @Getter
    private final String name;
    @Getter
    @Deprecated
    private final Types type;

    @Getter
    private final DataType dataType;

    public TableType(@NotNull String name, @NotNull DataType dataType) {
        this.name = name;
        this.type = null;
        this.dataType = dataType;
    }

    @Deprecated
    public TableType(@NotNull String name, @NotNull Types type) {
        this.name = name;
        this.type = type;
        this.dataType = null;
    }

    public String toTable() {
        if (dataType != null) {
            return name + " " + dataType;
        }
        return name + " " + type.toString();
    }
}

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
@Getter
public class TableType {

    private final String name;
    private final DataType dataType;
    private final boolean isIndex;

    public TableType(@NotNull String name, @NotNull DataType dataType) {
        this(name, dataType, false);
    }

    public TableType(@NotNull String name, @NotNull DataType dataType, boolean isIndex) {
        this.name = name;
        this.dataType = dataType;
        this.isIndex = isIndex;
    }

    public String toTable() {
        return name + " " + dataType;
    }
}

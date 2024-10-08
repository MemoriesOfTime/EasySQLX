package com.smallaswater.easysqlx.orm.api;


import com.smallaswater.easysqlx.orm.annotations.entity.Column;
import com.smallaswater.easysqlx.orm.utils.Kind;

import java.sql.Timestamp;

/**
 * 可以继承这个类，将以下字段内嵌入用户自定义的Entity内
 */
public class Entity {

    // 一个 Entity 一定要有一个主键
    @Column(name = "id", kind = Kind.ID)
    public long id;

    @Column(name = "create_at", kind = Kind.DATETIME)
    public Timestamp CreatedAt;

    @Column(name = "updated_at", kind = Kind.DATETIME)
    public Timestamp UpdatedAt;

}

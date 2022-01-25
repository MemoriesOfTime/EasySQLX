package com.smallaswater.easysql.orm.api;


import com.smallaswater.easysql.mysql.utils.Types;
import com.smallaswater.easysql.orm.annotations.entity.Column;

import java.sql.Timestamp;

/**
 * 可以继承这个类，将以下字段内嵌入用户自定义的Entity内
 */
public class Entity {

    // 一个 Entity 一定要有一个主键
    @Column(name = "id", type = Types.ID)
    public long id;

    @Column(name = "create_at", type = Types.DATETIME)
    public Timestamp CreatedAt;

    @Column(name = "updated_at", type = Types.DATETIME)
    public Timestamp UpdatedAt;

}

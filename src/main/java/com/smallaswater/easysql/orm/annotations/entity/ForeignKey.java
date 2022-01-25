package com.smallaswater.easysql.orm.annotations.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



/**
 * 外键(仅在创建数据表时有效)
 * 使用:
 *      @ForeignKey(tableName = "t_parent", columnName = "id")
 *      @Column(name = "id", type = Types.INT)
 *      public Long id;
 *
 * 会生成这样的 DDL
 * create table t_table(
 *      id int(10) not null,
 *      foreign key(id) references t_parent(id)
 * )
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ForeignKey {

    String tableName();  // 父表名称

    String columnName(); // 要关联父表中的字段名称

}

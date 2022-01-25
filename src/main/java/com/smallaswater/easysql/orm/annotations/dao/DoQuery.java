package com.smallaswater.easysql.orm.annotations.dao;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于返回多条数据 -> List
 * 用法:
 *      @DoQuery("SELECT * FROM {table}")
 *      List<ExampleEntity> queryEntities();
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DoQuery {
    String value();
}

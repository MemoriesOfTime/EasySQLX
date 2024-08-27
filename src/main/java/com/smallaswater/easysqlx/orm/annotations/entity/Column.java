package com.smallaswater.easysqlx.orm.annotations.entity;

import com.smallaswater.easysqlx.orm.utils.Kind;
import com.smallaswater.easysqlx.orm.utils.Option;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 给 Entity 上的字段加上，它就会被读取到 orm 内
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {

    String name(); // 数据表内列的名称

    Option[] options() default {}; // 格外参数

    Kind kind(); // 类型 (提供了一些常用的选项)
}

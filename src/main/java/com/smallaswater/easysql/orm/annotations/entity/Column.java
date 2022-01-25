package com.smallaswater.easysql.orm.annotations.entity;

import com.smallaswater.easysql.mysql.utils.Types;
import com.smallaswater.easysql.orm.utils.Options;

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

    Types type(); // 类型 (提供了一些常用的选项)

    Options[] options() default {}; // 格外参数

}

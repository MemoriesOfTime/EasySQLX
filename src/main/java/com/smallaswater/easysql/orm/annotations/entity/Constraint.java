package com.smallaswater.easysql.orm.annotations.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 表级定义约束(仅在创建数据表时有效)
 * 使用：
 *      @Constraint(name = "t_table_account_unique", type = ConstraintType.UNIQUE)
 *      @Column(name = "name", type = Types.VARCHAR)
 *      public String name;
 *
 *      @Constraint(name = "t_table_account_unique", type = ConstraintType.UNIQUE)
 *      @Column(name = "email", type = Types.VARCHAR)
 *      public String email;
 *
 * 会生成这样的 DDL
 * create table t_table(
 *      name varchar(255) not null,
 *      constraint t_table_account_unique unique(name, email)
 * )
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Constraint {

    String name(); // 约束的名称

    Type type(); // 约束类型

    enum Type {

        PRIMARY("primary"), // 希望有个联合主键可以用这个
        UNIQUE("unique");   // 希望多个列联合唯一可以用这个

        private final String sql;

        Type(String sql) {
            this.sql = sql;
        }

        @Override
        public String toString() {
            return this.sql;
        }
    }
}



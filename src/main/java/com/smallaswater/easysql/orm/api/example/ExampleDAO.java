package com.smallaswater.easysql.orm.api.example;

import com.smallaswater.easysql.orm.annotations.dao.*;
import com.smallaswater.easysql.orm.api.IDAO;

import java.util.List;


public interface ExampleDAO extends IDAO<ExampleEntity> {

    @DoInsert // DoInsert 注释的方法只支持一个参数，并且为范型类型
    void insert(ExampleEntity entity);

    @DoQuery("SELECT * FROM {table}") // {table} 会在执行时替换成Builder内的table
    List<ExampleEntity> queryEntities();

    /**
     * ? 为参数的占位符，多少个参数就多少个 ?，? 在什么位置参数就会补充到什么位置
     */
    @DoQuery("SELECT * FROM {table} WHERE id > ?")
    List<ExampleEntity> queryEntitiesIDAfter(Long id);

    @DoQueryRow("SELECT * FROM {table} WHERE id = ?")
    ExampleEntity queryEntity(Long id);

    @DoExecute("DELETE FROM {table} WHERE id = ? AND uuid = ?")
    void deleteEntity(Long id, String uuid);

    /**
     * 底层使用的是 javassist 生成代理类来执行这个方法
     */
    @DoDefault
    default void doSomething() {
        getManager(); // you got sqlManager
        getTable(); // you got tableName
        // and do something you like!
    }

}

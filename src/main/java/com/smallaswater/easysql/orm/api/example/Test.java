package com.smallaswater.easysql.orm.api.example;

import com.smallaswater.easysql.mysql.utils.UserData;
import com.smallaswater.easysql.orm.api.ORMHandleBuilder;

import java.util.List;

public class Test {


    public static void main(String[] args) {


        ORMHandleBuilder builder = ORMHandleBuilder.builder(null/*your pluginBase*/, "test");
        ExampleDAO dao = ((ExampleDAO) builder.fromUserData(new UserData("root", "114514", "localhost", 3306, "test_db"))
                .buildHost()
                .buildORMDynaProxyIDAO(ExampleDAO.class));


        ExampleEntity entity = new ExampleEntity();
        entity.name = "hahaha";
        entity.index = 114514L;
        dao.insert(entity); // doInsert 会在 ExampleEntity 表不存在时创建一个

        dao.deleteEntity(1L, "your uuid");  // 删掉这个数据
        List<ExampleEntity> got = dao.queryEntitiesIDAfter(2L);// 查询一些你想要的数据

        dao.doSomething();  // 做一些你想做的事情

    }
}

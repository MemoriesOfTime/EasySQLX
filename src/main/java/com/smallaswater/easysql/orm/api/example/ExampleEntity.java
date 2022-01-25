package com.smallaswater.easysql.orm.api.example;

import com.smallaswater.easysql.mysql.utils.Types;
import com.smallaswater.easysql.orm.annotations.entity.*;
import com.smallaswater.easysql.orm.utils.Options;

public class ExampleEntity {

    @Column(name = "id", type = Types.ID) // ID 类型的 column 会自增
    public Long id;

    @Constraint(name = "t_unique_uuids", type = Constraint.Type.UNIQUE)
    @AutoUUIDGenerate // 自动生成 uuid
    @Column(name = "uuid", type = Types.VARCHAR)
    //@ForeignKey(tableName = "t_other", columnName = "uuid")  外键
    public String uuid;

    @Column(name = "register_index", type = Types.INT, options = {Options.NULL}) // 注意name字段不要命名成关键字，如这里的 index
    public Long index;

    @Constraint(name = "t_unique_uuids", type = Constraint.Type.UNIQUE)
    @AutoUUIDGenerate
    @Column(name = "second_uuid", type = Types.VARCHAR, options = {Options.NULL})  // 可以是 null
    public String secondUUID;

    @Column(name = "name", type = Types.VARCHAR)
    public String name;

    @Override
    public String toString() {
        return "ExampleEntity{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", index=" + index +
                ", secondUUID='" + secondUUID + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

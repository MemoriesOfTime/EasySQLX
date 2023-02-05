package ormexample;

import com.smallaswater.easysql.orm.annotations.entity.*;
import com.smallaswater.easysql.orm.utils.Kind;
import com.smallaswater.easysql.orm.utils.Option;

public class ExampleEntity {

    @Column(name = "id", kind = Kind.ID) // ID 类型的 column 会自增
    public Long id;

    @Constraint(name = "t_unique_uuids", type = Constraint.Type.UNIQUE)
    @AutoUUIDGenerate // 自动生成 uuid
    @Column(name = "uuid", kind = Kind.VARCHAR)
    //@ForeignKey(tableName = "t_other", columnName = "uuid")  外键
    public String uuid;

    @Column(name = "register_index", kind = Kind.INT, options = {Option.NULL}) // 注意name字段不要命名成关键字，如这里的 index
    public Long index;

    @Constraint(name = "t_unique_uuids", type = Constraint.Type.UNIQUE)
    @AutoUUIDGenerate
    @Column(name = "second_uuid", kind = Kind.VARCHAR, options = {Option.NULL})  // 可以是 null
    public String secondUUID;

    @Column(name = "name", kind = Kind.VARCHAR)
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

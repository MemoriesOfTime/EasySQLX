![easysql_logo](./img/mysql.png)

<p align="center">
    <a href="https://github.com/SmallasWater/EasyMySQL/releases/latest" alt="Latest release">
        <img src="https://img.shields.io/github/v/release/SmallasWater/EasyMySQL?include_prereleases" alt="Latest release">
    </a>
</p>

**EasySQL** 是一个用于简化 MySQL 数据库操作的插件。使用本插件前，建议先了解 MySQL 的基本概念，尤其是**字段**和**表名**的含义。

## MySQL 部分

### 核心类与方法

#### `SqlManager` 类

`SqlManager` 是本插件的核心类，用于管理数据库连接和执行操作。

**构造方法参数：**

- `plugin`: 插件的 `pluginBase` 类实例。
- `data`: 包含数据库账号、密码等信息的 `UserData` 对象。

**常用方法：**

- `isEnable()`: 返回数据库是否连接成功。
- `disable()`: 关闭数据库连接，通常用于插件的 `onDisable` 方法中。
- `getConnection()`: 获取 `Connection` 对象。
- `executeSql(String sql, ChunkSqlType... value)`: 执行 SQL 语句。
- `executeFunction(MySqlFunctions functions)`: 执行 MySQL 函数。
- `executeFunction(String functions)`: 执行 MySQL 函数。
- `isExistTable(String tableName)`: 检查表是否存在。
- `createTable(String tableName)`: 创建一个仅包含自增 ID 的表。
- `createTable(String tableName, TableType... tableTypes)`: 根据指定参数创建表。
- `deleteTable(String tableName)`: 删除指定表。
- `isExistColumn(String table, String column)`: 检查字段（列）是否存在。
- `createColumn(String tableName, TableType tableType)`: 创建字段（列）。
- `deleteColumn(String tableName, String args)`: 删除字段（列）。
- `isExistsData(String tableName, String column, String data)`: 检查是否存在指定数据。
- `setData(String tableName, SqlData data, SqlData where)`: 修改数据。
- `insertData(String tableName, SqlData data)`: 插入单条数据。
- `insertData(String tableName, LinkedList<SqlData> datas)`: 插入多条数据。
- `deleteData(String tableName, SqlData data)`: 删除数据。
- `getDataSize(String sql, String tableName, ChunkSqlType... sqlType)`: 获取数据条数。
- `getData(String tableName, SelectType selectType)`: 获取数据。
- `getData(String sql, ChunkSqlType... types)`: 获取数据。

#### `SqlData` 类

`SqlData` 用于存储查询结果或作为写入数据的参数。

**参数：**

- `column`: 字段名。
- `object`: 读取到的数值。

**常用方法：**

- `put(String column, Object object)`: 写入数据，`column` 为字段名，`object` 为要写入的数据。
- `get(String column, T defaultValue)`: 根据字段名获取数据，`T` 为返回值为 `null` 时的默认类型。
- `getColumns()`: 获取所有字段名称。
- `getColumn(int index)`: 根据索引获取对应的字段名。

### 使用方法

#### 1. 连接数据库

首先，实例化 `SqlManager` 类并连接数据库：

```java
SqlManager manager = new SqlManager(plugin, userData);
```

#### 2. 执行 SQL 操作

使用 `SqlManager` 提供的方法执行 SQL 操作，例如创建表、插入数据、查询数据等。

```java
// 创建表
manager.createTable("example_table",new TableType("id", "INT AUTO_INCREMENT PRIMARY KEY"));

// 插入数据
SqlData data = new SqlData();
data.

put("name","John Doe");
data.

put("age",30);
manager.

insertData("example_table",data);

// 查询数据
SqlData result = manager.getData("example_table", SelectType.ALL);
```

## ORM 部分

> 如果你习惯使用 **orm** 来操作数据库的话，EasyMySQL也提供了一个 orm 框架 (效率并不高)
>
> 使用 orm 能够更加的快捷方便的操作数据

+ Example is all you need

> 先创建一个 **Entity** (这里的 Entity 不是 nk 里的实体，而是 orm 里面的数据对象)
>
> 例如:
>
> ```java
> public class ExampleEntity {
> 
>     @Column(name = "id", type = Types.ID) // ID 类型的 column 会自增
>     public Long id;
> 
>     @Constraint(name = "t_unique_uuids", type = Constraint.Type.UNIQUE)
>     @AutoUUIDGenerate // 自动生成 uuid
>     @Column(name = "uuid", type = Types.VARCHAR)
>     //@ForeignKey(tableName = "t_other", columnName = "uuid")  外键
>     public String uuid;
> 
>     @Column(name = "register_index", type = Types.INT, options = {Options.NULL}) // 注意name字段不要命名成关键字，如这里的 index
>     public Long index;
> 
>     @Constraint(name = "t_unique_uuids", type = Constraint.Type.UNIQUE)
>     @AutoUUIDGenerate
>     @Column(name = "second_uuid", type = Types.VARCHAR, options = {Options.NULL})  // 可以是 null
>     public String secondUUID;
> 
>     @Column(name = "name", type = Types.VARCHAR)
>     public String name;
> 
> }
> ```
>
>
更多的注解请看: https://github.com/iGxnon/EasyMySQL/tree/feat-orm/src/main/java/com/smallaswater/easysql/orm/annotations/entity

> 然后创建一个 接口(interface) 去继承我们的 **com.smallaswater.easysqlx.orm.api.IDAO**
>
> IDAO的范型需要确定为你上面定义的 Entity
>
> 例如:
>
> ```java
> public interface ExampleDAO extends IDAO<ExampleEntity> {
> 
>     @DoInsert // DoInsert 注释的方法只支持一个参数，并且为范型类型
>     void insert(ExampleEntity entity);
> 
>     @DoQuery("SELECT * FROM {table}") // {table} 会在执行时替换成Builder内的table
>     List<ExampleEntity> queryEntities();
> 
>     /**
>      * ? 为参数的占位符，多少个参数就多少个 ?，? 在什么位置参数就会补充到什么位置
>      */
>     @DoQuery("SELECT * FROM {table} WHERE id > ?")
>     List<ExampleEntity> queryEntitiesIDAfter(Long id);
> 
>     @DoQueryRow("SELECT * FROM {table} WHERE id = ?")
>     ExampleEntity queryEntity(Long id);
> 
>     @DoExecute("DELETE FROM {table} WHERE id = ? AND uuid = ?")
>     void deleteEntity(Long id, String uuid);
> 
>     /**
>      * 底层使用的是 javassist 生成代理类来执行这个方法
>      */
>     @DoDefault
>     default void doSomething() {
>         getManager(); // you got sqlManager
>         getTable(); // you got tableName
>         // and do something you like!
>     }
> 
> }
> ```
>
> 在这个接口内，你可以使用 orm 提供的一些注解来 ‘注释’ 你的操作，如果你了解 SQL 语句的话，这会十分易懂
>
> 你只需模仿这个例子，写一个自己的 IDAO 就行了！
>
> 为什么只需要写一个接口？因为 orm 帮你把接口实现了！
>
>
更多注解请看: https://github.com/iGxnon/EasyMySQL/tree/feat-orm/src/main/java/com/smallaswater/easysql/orm/annotations/dao

> 最后，就像这样就能使用了!
>
> ```java
> public static void main(String[] args) {
> 
>         ORMHandleBuilder builder = ORMHandleBuilder.builder(null/*your pluginBase*/, "test");
>         ExampleDAO dao = ((ExampleDAO) builder.fromUserData(new UserData("root", "114514", "localhost", 3306, "test_db")) // 连接你的数据库
>                 .buildHost()  // 这一步不能省略
>                 .buildORMDynaProxyIDAO(ExampleDAO.class));  // 传入你的 IDAO 接口，然后强转成那个类型
> 
>         ExampleEntity entity = new ExampleEntity();
>         entity.name = "hahaha";
>         entity.index = 114514L;
>         dao.insert(entity); // doInsert 会在 ExampleEntity 表不存在时创建一个
> 
>         dao.deleteEntity(1L, "your uuid");  // 删掉这个数据
>         List<ExampleEntity> got = dao.queryEntitiesIDAfter(2L);// 查询一些你想要的数据
> 
>         dao.doSomething();  // 做一些你想做的事情
> 
> }
> ```
>
> 

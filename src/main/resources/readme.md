<a href="https://github.com/SmallasWater/EasyMySQL/releases/latest" alt="Latest release">
    <img src="https://img.shields.io/github/v/release/SmallasWater/EasyMySQL?include_prereleases" alt="Latest release">
</a>


#EasySQL

> 使用本插件可以更方便的使用MySQL数据库
> 使用本插件需要先了解一定的MySQL 至少要知道 **字段**，**表名** 的意思

> * 使用方法:
>   - 实例化 **SqlEnable**, **SqlManager** 或者 **UseTableSqlManager** 任意一个均可连接数据库
>   ~~~
>  
>   /**
>    * 连接数据表
>    */
>   SqlManager manager = new SqlManager(Plugin, UserData);
>   
>     ~~~
~~~~
 * 用法简介
    * SqlManager 类: 使用本插件需实例化本类连接数据库
      
      参数:
       - plugin: 插件的 pluginBase 类即可
       - data: 数据库账号密码等数据 具体参考 **UserData** 构造方法

      方法:
       - isEnable(): 返回数据库是否连接成功
       - disable(): 关闭数据库的连接 一般在插件onDisable 内使用
       - getConnection(): 获取**Connection**
       - executeSql(String sql, ChunkSqlType... value): 执行sql语句
       - executeFunction(MySqlFunctions functions): 执行MySQL函数
       - executeFunction(String functions): 执行MySQL函数
       - isExistTable(String tableName): 是否存在表
       - createTable(String tableName): 创建一个只包含自增ID的表
       - createTable(String tableName, TableType... tableTypes): 根据参数创建表
       - deleteTable(String tableName): 删除表
       - isExistColumn(String table, String column): 是否存在字段(列)  
       - createColumn(String tableName, TableType tableType): 创建字段(列)
       - deleteColumn(String tableName, String args): 删除字段(列)
       - isExistsData(String tableName, String column, String data): 是否存在数据
       - setData(String tableName, SqlData data, SqlData where): 修改数据
       - insertData(String tableName, SqlData data): 添加数据
       - insertData(String tableName, LinkedList<SqlData> datas): 添加多条数据
       - deleteData(String tableName, SqlData data): 删除数据
       - getDataSize(String sql, String tableName, ChunkSqlType... sqlType): 获取数据条数
       - getData(String tableName, SelectType selectType): 获取数据
       - getData(String sql, ChunkSqlType... types): 获取数据
   ~~~~
    * SqlData: 为查询语句获取的返回值 或者为向 MySQL写入数据用到的参数

      参数:
       - column: 字段名
       - object: 读取到的数值

      方法:
       - put(String column,Object object): 写入数据 column 为字段名 object 为要写入的数据
       - get(String column,T defaultValue): 根据字段名获取 任意类型数据 T 为返回值为null的情况下的类型
       - getColumns(): 获取返回的所有字段名称
       - getColumn(int index): 根据索引获取对应的字段名




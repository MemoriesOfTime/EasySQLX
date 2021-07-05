<a href="https://github.com/SmallasWater/EasyMySQL/releases/latest" alt="Latest release">
    <img src="https://img.shields.io/github/v/release/SmallasWater/EasyMySQL?include_prereleases" alt="Latest release">
</a>
<br>
 

# EasySQL

> 使用本插件可以更方便的使用MySQL数据库
> 使用本插件需要先了解一定的MySQL 至少要知道 **字段**，**表名** 的意思
> 关键词: column: 字段名

> * 使用方法:
>   - 实例化 **SqlEnable**, **SqlManager** 或者 **UseTableSqlManager** 任意一个均可连接数据库
>   ~~~
>  
>   /**
>    * 连接数据表
>    * 若增加 types 参数，则创建数据库
>    */
>   SqlManager manager = new SqlManager(Plugin,"表单名称", UserData); 
>   ~~~
>   - 调用数据库对象 SqlManager 这里封装了大部分的MySQL指令
>   ~~~
>   SqlManager manager = new SqlManager(Plugin,"表单名称", UserData, TableType... type);
>   /**
>     * 获取 SQLDataManager
>     */
>    SQLDataManager sdm = manager.getSqlManager();
>   
>     ~~~
 * 用法简介
    * SqlEnable 类(SqlManager,UseTableSqlManager 用法基本一致): API类。使用本插件需实例化本类连接数据库
      
      参数:
       - plugin: 插件的 pluginBase 类即可
       - tableName: 数据库的表单名称
       - data: 用户数据 具体参考 **UserData** 构造方法
       - table: 创建数据表需要用到的参数 (可不填 若没有创建相应的数据表，则这个为必填项)

      方法:
       - disable(): 关闭数据库的连接 一般在插件onDisable 内使用
       - getManager(): 获取 SqlManager 类后进行一系列操作
       - getData(): 获取 UserData 类
    * SqlData: 为查询语句获取的返回值 或者为向 MySQL写入数据用到的参数

      参数:
       - column: 字段名
       - object: 读取到的数值

      方法 (其他方法请参考 JavaDoc):
       - put(String column,Object object): 写入数据 column 为字段名 object 为要写入的数据
       - get(String column,T defaultValue): 根据字段名获取 任意类型数据 T 为返回值为null的情况下的类型
       - getColumns(): 获取返回的所有字段名称
       - getColumn(int index): 根据索引获取对应的字段名
       - getObjects(): 获取返回所有的数据
     * SqlDataList 类: 查询语句返回的数据列表
     
       参数:
        - sql: SQL语句
        - types[] : 参考 ChunkType 类用法
       
       方法:
        - getValueList(column): 根据字段获取全部的数据
        - get(): 获取返回列表中的第一行数据 没有数据则返回实例化的SqlData
        - getSql(): 获取执行的SQL语句
     * SqlDataManager 类:
      
       参数: **不建议实例化本类 请通过 getManager() 方法获取**
        
       方法: 
        - selectExecute(): 执行查询语句 详情请参考JavaDoc
        - isExists(column,data): 判断数据是否存在于数据库
        - deleteData(data): 删除数据 data 为SqlData 使用方法参考 SqlData
        - insertData(datas): 添加新的数据
        - runSql(sql,type): 执行SQL语句 无返回数据 只返回是否执行成功
        - isTableColumnData(tableName): 判断表单是否存在
        - setData(data,where): 修改数据 前者为要修改的值 后者为判断条件
        - executeFunction(functions): 执行单行函数 (其实Java就能实现)
     * ChunkSqlType 类 防止SQL注入:
     
       参数:
        - i: `?` 的位置从1开始 用作 `=` 后面
        - value : 要写入的数据
     




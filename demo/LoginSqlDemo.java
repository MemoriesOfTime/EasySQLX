package org.easymysql.demo;

import cn.nukkit.plugin.PluginBase;
import com.smallaswater.easysqlx.exceptions.MySqlLoginException;
import com.smallaswater.easysqlx.mysql.data.SqlData;
import com.smallaswater.easysqlx.mysql.data.SqlDataList;
import com.smallaswater.easysqlx.mysql.utils.TableType;
import com.smallaswater.easysqlx.mysql.utils.UserData;
import com.smallaswater.easysqlx.mysql.manager.SqlManager;
import com.smallaswater.easysqlx.v3.mysql.utils.SelectType;

/**
 * 连接mysql
 * @author SmallasWater
 */
public class LoginSqlDemo extends PluginBase {


    public static SqlManager sqlManager;

    public static final String HOST = "数据库ip地址";

    public static final String DATABASE = "数据库名";

    public static final int PORT = 3306;

    public static final String USER = "用户名";

    public static final String PASS_WORLD = "密码";


    @Override
    public void onEnable() {
        sqlManager = login();
        if(sqlManager != null){
            this.getLogger().info("数据库连接成功");
        }else{
            this.getLogger().info("数据库连接失败");
            return;
        }

        //TODO 创建表单 字段可以通过Types 选择其他数据类型
        String tableName = "表单名称";
        TableType[] tableTypes = new TableType[3];
        tableTypes[0] = new TableType("字段1", Types.TEXT);
        tableTypes[0] = new TableType("字段2", Types.TEXT);
        tableTypes[0] = new TableType("字段3", Types.TEXT);
        if(sqlManager.createTable(tableName,tableTypes)){
            this.getLogger().info("数据库表创建成功");
        }else{
            this.getLogger().info("数据库表创建失败");
        }
        //TODO 增删改查
        //添加内容
        SqlData addData = new SqlData();
        addData.put("字段1","内容 其他非基础数据类型在存储时会转换为字符串");
        addData.put("字段2","内容 其他非基础数据类型在存储时会转换为字符串");
        addData.put("字段3","内容 其他非基础数据类型在存储时会转换为字符串");
        sqlManager.insertData(tableName,addData);
        //删除内容
        SqlData delData = new SqlData();
        delData.put("字段1","你要删除的内容");
        delData.put("字段2","你要删除的内容");
        delData.put("字段3","你要删除的内容");
        sqlManager.deleteData(tableName,delData);
        //修改内容
        SqlData changeData1 = new SqlData();
        changeData1.put("字段1","旧的内容");
        changeData1.put("字段2","旧的内容");
        changeData1.put("字段3","旧的内容");
        SqlData changeData2 = new SqlData();
        changeData1.put("字段1","你要修改的内容");
        changeData1.put("字段2","你要修改的内容");
        changeData1.put("字段3","你要修改的内容");
        sqlManager.setData(tableName,changeData1,changeData2);
        //查找内容
        SelectType type = new SelectType("字段1","数据库要查询的内容");
        //SELECT * FROM tabName where playerName = ?
        SqlDataList<SqlData> dataList = sqlManager.getData(tableName,type);
        

    }

    /**
     * 连接数据库并获取{@link SqlManager}SqlManager实例化对象
     * @return SqlManager
     * */
    public SqlManager login(){
        try{
            return new SqlManager(this,new UserData(
                    USER,
                    PASS_WORLD,
                    HOST,
                    PORT,
                    DATABASE
            ));
        }catch (MySqlLoginException e){
            this.getLogger().info(e.getMessage());
        }
        return null;



    }

    @Override
    public void onDisable() {
        sqlManager.disable();
    }
}

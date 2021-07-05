package com.smallaswater.easysql.mysql.utils;


/**
 * @author SmallasWater
 * @create 2020/9/30 23:33
 */
public class MySqlFunctions {

    private final SqlFunctions functions;

    private final String column;

    public static final String COUNT = "COUNT(?)";
    public static final String AVG = "AVG(?)";
    public static final String MIN = "MIN(?)";
    public static final String MAX = "MAX(?)";
    public static final String SUM = "SUM(?)";


    public static final String MD5 = "md5(?)";


    public MySqlFunctions(SqlFunctions functions, String column) {
        this.functions = functions;
        this.column = column;
    }


    public static String getFunction(SqlFunctions functions, String column) {
        return functions.getCommand().replace("?", column);

    }

    public String getCommand() {
        return "SELECT " + functions.getCommand().replace("?", column) + ";";
    }

    public enum SqlFunctions {
        /**
         * 获取数据数量
         */
        COUNT(MySqlFunctions.COUNT),
        /**
         * 获取数据平均值
         */
        AVG(MySqlFunctions.AVG),
        /**
         * 获取数据最小值
         */
        MIN(MySqlFunctions.MIN),
        /**
         * 获取数据最大值
         */
        MAX(MySqlFunctions.MAX),
        /**
         * 获取数据总和
         */
        SUM(MySqlFunctions.SUM),
        /**
         * 通过md5 加密字符串
         */
        MD5(MySqlFunctions.MD5);


        protected String command;

        SqlFunctions(String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }
    }

}

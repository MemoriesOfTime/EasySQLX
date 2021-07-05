package com.smallaswater.easysql.exceptions;

/**
 * @author SmallasWater
 */
public class MySqlLoginException extends Exception {
    @Override
    public String getMessage() {
        return "数据库连接失败，请检查信息是否填写错误";
    }
}

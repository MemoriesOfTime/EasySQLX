package com.smallaswater.easysql.exceptions;

/**
 * @author SmallasWater
 */
public class MySqlLoginException extends Exception {

    public MySqlLoginException() {
        this("数据库连接失败，请检查信息是否填写错误");
    }

    public MySqlLoginException(String message) {
        super(message);
    }

}

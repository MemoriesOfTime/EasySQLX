package com.smallaswater.easysqlx.orm.api;

import com.smallaswater.easysqlx.mysql.manager.SqlManager;

public interface IDAO<T> {

    SqlManager getManager();

    String getTable();

}

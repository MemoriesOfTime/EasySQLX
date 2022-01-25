package com.smallaswater.easysql.orm.api;

import com.smallaswater.easysql.v3.mysql.manager.SqlManager;

public interface IDAO<T> {

    SqlManager getManager();

    String getTable();

}

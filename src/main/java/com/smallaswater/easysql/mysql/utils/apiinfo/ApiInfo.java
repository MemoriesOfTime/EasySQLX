package com.smallaswater.easysql.mysql.utils.apiinfo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author LT_Name
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.FIELD,
        ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PACKAGE,
        ElementType.LOCAL_VARIABLE, ElementType.TYPE_PARAMETER, ElementType.TYPE_USE,
        ElementType.PARAMETER})
public @interface ApiInfo {

    String value();

}

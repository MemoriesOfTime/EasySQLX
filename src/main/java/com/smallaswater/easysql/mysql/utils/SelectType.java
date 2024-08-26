package com.smallaswater.easysql.mysql.utils;

import lombok.Getter;

/**
 * @author lt_name
 */
@Getter
public class SelectType {

    private final String key;
    private final String value;
    private final Types type;

    public SelectType(String key, String value) {
        this(key, value, Types.EQUAL);
    }

    public SelectType(String key, String value, Types type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    @Override
    public String toString() {
        return " WHERE " + this.key + " " + this.type.getSymbol() + " ? ";
    }

    public enum Types {

        EQUAL("="),
        UNEQUAL("!="),
        GREATER(">"),
        LESS("<"),
        EQUAL_OR_GREATER(">="),
        LESS_OR_EQUAL("<=");

        @Getter
        private final String symbol;

        Types(String symbol) {
            this.symbol = symbol;
        }

    }

}

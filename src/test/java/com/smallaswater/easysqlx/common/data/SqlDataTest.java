package com.smallaswater.easysqlx.common.data;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SqlDataTest {

    @Test
    public void classToSqlDataUsesClassValueFieldCacheInsteadOfStrongClassMap() throws Exception {
        Field cacheField = SqlData.class.getDeclaredField("FIELD_CACHE");

        assertEquals(ClassValue.class, cacheField.getType());
        assertFalse(Map.class.isAssignableFrom(cacheField.getType()));
    }

    @Test
    public void classToSqlDataKeepsPublicFieldConversionBehavior() {
        TestRow row = new TestRow();
        row.id = 7;
        row.name = "Alice";
        row.score = 42;

        SqlData data = SqlData.classToSqlData(row);

        assertFalse(data.getData().containsKey("id"));
        assertEquals("Alice", data.getString("name"));
        assertEquals(42, data.getInt("score"));
    }

    @Test
    public void classToSqlDataAsIdIncludesIdField() {
        TestRow row = new TestRow();
        row.id = 7;
        row.score = 42;

        SqlData data = SqlData.classToSqlDataAsId(row);

        assertTrue(data.getData().containsKey("id"));
        assertEquals(7, data.getInt("id"));
        assertEquals(42, data.getInt("score"));
    }

    public static class TestRow {
        public int id;
        public String name;
        public int score;
    }
}

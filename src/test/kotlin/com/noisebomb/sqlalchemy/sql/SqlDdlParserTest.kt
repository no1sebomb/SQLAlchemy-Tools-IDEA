package com.noisebomb.sqlalchemy.sql

import com.noisebomb.sqlalchemy.model.SqlAlchemyColumnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlDdlParserTest {

    private fun success(sql: String): ParsedTable {
        val result = SqlDdlParser.parse(sql)
        assertTrue("Expected success but got: $result", result is SqlParseResult.Success)
        return (result as SqlParseResult.Success).table
    }

    @Test
    fun blankInputIsEmpty() {
        assertTrue(SqlDdlParser.parse("   ") is SqlParseResult.Empty)
    }

    @Test
    fun garbageIsFailure() {
        assertTrue(SqlDdlParser.parse("not sql at all ;;;") is SqlParseResult.Failure)
    }

    @Test
    fun parsesInlineConstraintsAndTypes() {
        val table = success(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY,
                email VARCHAR(255) NOT NULL UNIQUE,
                age INT,
                bio TEXT,
                active BOOLEAN NOT NULL DEFAULT TRUE,
                score NUMERIC(10, 2) DEFAULT 0,
                created_at TIMESTAMP
            )
            """.trimIndent()
        )

        assertEquals("users", table.tableName)
        assertEquals(7, table.columns.size)

        val id = table.columns[0]
        assertEquals("id", id.name)
        assertEquals(SqlAlchemyColumnType.INTEGER, id.type)
        assertTrue(id.primaryKey)
        assertFalse(id.nullable)

        val email = table.columns[1]
        assertEquals(SqlAlchemyColumnType.STRING, email.type)
        assertFalse(email.nullable)
        assertTrue(email.unique)

        val age = table.columns[2]
        assertTrue(age.nullable)

        assertEquals(SqlAlchemyColumnType.TEXT, table.columns[3].type)

        val active = table.columns[4]
        assertEquals(SqlAlchemyColumnType.BOOLEAN, active.type)
        assertEquals("True", active.defaultExpression)

        val score = table.columns[5]
        assertEquals(SqlAlchemyColumnType.NUMERIC, score.type)
        assertEquals("0", score.defaultExpression)

        assertEquals(SqlAlchemyColumnType.DATETIME, table.columns[6].type)
    }

    @Test
    fun parsesTableLevelPrimaryKey() {
        val table = success(
            """
            CREATE TABLE orders (
                order_id BIGINT,
                customer VARCHAR(100),
                PRIMARY KEY (order_id)
            )
            """.trimIndent()
        )
        val pk = table.columns.first { it.name == "order_id" }
        assertEquals(SqlAlchemyColumnType.BIG_INTEGER, pk.type)
        assertTrue(pk.primaryKey)
        assertFalse(pk.nullable)
    }

    @Test
    fun dropsNonPythonDefaults() {
        val table = success(
            """
            CREATE TABLE events (
                id INTEGER PRIMARY KEY,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )
        // CURRENT_TIMESTAMP cannot be expressed as a Python literal, so it is dropped.
        assertEquals("", table.columns.first { it.name == "created_at" }.defaultExpression)
    }

    @Test
    fun handlesQuotedIdentifiersAndFirstTableOnly() {
        val table = success(
            """
            CREATE TABLE "public_users" ("Id" INTEGER PRIMARY KEY, "Email" VARCHAR(50));
            CREATE TABLE ignored (x INT);
            """.trimIndent()
        )
        assertEquals("public_users", table.tableName)
        assertEquals("Id", table.columns[0].name)
    }
}

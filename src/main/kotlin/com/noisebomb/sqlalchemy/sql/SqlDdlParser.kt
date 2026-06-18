package com.noisebomb.sqlalchemy.sql

import com.noisebomb.sqlalchemy.model.SqlAlchemyColumnType
import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.create.table.CreateTable
import net.sf.jsqlparser.statement.create.table.Index

/** A single column extracted from a CREATE TABLE statement. */
data class ParsedColumn(
    val name: String,
    val type: SqlAlchemyColumnType,
    val primaryKey: Boolean,
    val nullable: Boolean,
    val unique: Boolean,
    val defaultExpression: String,
    val comment: String
)

/** A table extracted from a CREATE TABLE statement. */
data class ParsedTable(
    val tableName: String,
    val columns: List<ParsedColumn>
)

/** Outcome of parsing a chunk of DDL. */
sealed interface SqlParseResult {
    /** Input was blank; nothing to do. */
    object Empty : SqlParseResult
    data class Success(val table: ParsedTable) : SqlParseResult
    data class Failure(val message: String) : SqlParseResult
}

/**
 * Parses CREATE TABLE DDL into a [ParsedTable] using the bundled JSqlParser, so SQL mode
 * works on every IDE regardless of whether the (paid) Database Tools and SQL plugin is present.
 *
 * Only the first CREATE TABLE statement is used (one model maps to one table). Foreign keys are
 * ignored for now — relationships are a separate, upcoming feature.
 */
object SqlDdlParser {

    fun parse(sql: String): SqlParseResult {
        if (sql.isBlank()) return SqlParseResult.Empty

        val createTable = try {
            extractCreateTable(sql)
        } catch (e: JSQLParserException) {
            return SqlParseResult.Failure(firstLine(e.message))
        } catch (e: Throwable) {
            return SqlParseResult.Failure(e.message ?: "Could not parse SQL")
        } ?: return SqlParseResult.Failure("No CREATE TABLE statement found")

        return try {
            SqlParseResult.Success(buildTable(createTable))
        } catch (e: Throwable) {
            SqlParseResult.Failure(e.message ?: "Could not read table definition")
        }
    }

    /** Prefer multi-statement parsing (tolerates trailing DDL); fall back to a single statement. */
    private fun extractCreateTable(sql: String): CreateTable? {
        val statements = try {
            CCJSqlParserUtil.parseStatements(sql).statements
        } catch (e: JSQLParserException) {
            listOf(CCJSqlParserUtil.parse(sql))
        }
        return statements.filterIsInstance<CreateTable>().firstOrNull()
    }

    private fun buildTable(ct: CreateTable): ParsedTable {
        // Table-level PRIMARY KEY / UNIQUE constraints, e.g. PRIMARY KEY (id) or UNIQUE (email).
        val pkColumns = HashSet<String>()
        val uniqueColumns = HashSet<String>()
        ct.indexes?.forEach { index ->
            val type = index.type?.uppercase().orEmpty()
            val names = indexColumnNames(index).map { unquote(it).lowercase() }
            when {
                type.contains("PRIMARY") -> pkColumns += names
                type == "UNIQUE" -> uniqueColumns += names
            }
        }

        val columns = (ct.columnDefinitions ?: emptyList()).map { def ->
            val name = unquote(def.columnName)
            val specs = def.columnSpecs ?: emptyList()

            var nullable = true
            var primaryKey = false
            var unique = false
            var default = ""
            var comment = ""

            // Column specs arrive as a flat token list, e.g. ["NOT", "NULL", "DEFAULT", "0"].
            var i = 0
            while (i < specs.size) {
                when (specs[i].uppercase()) {
                    "NOT" -> if (specs.getOrNull(i + 1).equals("NULL", true)) { nullable = false; i++ }
                    "NULL" -> nullable = true
                    "PRIMARY" -> if (specs.getOrNull(i + 1).equals("KEY", true)) { primaryKey = true; i++ }
                    "UNIQUE" -> unique = true
                    "DEFAULT" -> specs.getOrNull(i + 1)?.let { default = mapDefault(it); i++ }
                    "COMMENT" -> specs.getOrNull(i + 1)?.let { comment = unquoteString(it); i++ }
                }
                i++
            }

            val key = name.lowercase()
            if (key in pkColumns) primaryKey = true
            if (key in uniqueColumns) unique = true
            if (primaryKey) nullable = false

            ParsedColumn(
                name = name,
                type = mapType(def.colDataType?.dataType ?: ""),
                primaryKey = primaryKey,
                nullable = nullable,
                unique = unique,
                defaultExpression = default,
                comment = comment
            )
        }

        return ParsedTable(unquote(ct.table.name), columns)
    }

    private fun indexColumnNames(index: Index): List<String> = try {
        index.columns?.map { it.columnName } ?: emptyList()
    } catch (e: Throwable) {
        emptyList()
    }

    /** Maps a raw SQL type name to the closest [SqlAlchemyColumnType]. */
    private fun mapType(rawType: String): SqlAlchemyColumnType {
        val t = rawType.uppercase().substringBefore('(').trim()
        return when {
            t in setOf("BIGINT", "INT8", "BIGSERIAL", "SERIAL8") -> SqlAlchemyColumnType.BIG_INTEGER
            t in setOf("INT", "INTEGER", "INT4", "INT2", "SERIAL", "SMALLSERIAL",
                "SMALLINT", "TINYINT", "MEDIUMINT") -> SqlAlchemyColumnType.INTEGER
            t == "BOOLEAN" || t == "BOOL" || t == "BIT" -> SqlAlchemyColumnType.BOOLEAN
            t.contains("DOUBLE") || t in setOf("FLOAT", "REAL", "FLOAT4", "FLOAT8",
                "BINARY_FLOAT", "BINARY_DOUBLE") -> SqlAlchemyColumnType.FLOAT
            t in setOf("NUMERIC", "DECIMAL", "DEC", "NUMBER", "MONEY", "SMALLMONEY") -> SqlAlchemyColumnType.NUMERIC
            t == "DATE" -> SqlAlchemyColumnType.DATE
            t.contains("TIMESTAMP") || t in setOf("DATETIME", "DATETIME2", "SMALLDATETIME") -> SqlAlchemyColumnType.DATETIME
            t.startsWith("TIME") -> SqlAlchemyColumnType.TIME
            t.contains("JSON") -> SqlAlchemyColumnType.JSON
            t == "UUID" || t == "UNIQUEIDENTIFIER" -> SqlAlchemyColumnType.UUID
            t.contains("CHAR") || t in setOf("VARCHAR2", "NVARCHAR", "NCHAR", "STRING") -> SqlAlchemyColumnType.STRING
            t.contains("TEXT") || t in setOf("CLOB", "NCLOB", "NTEXT") -> SqlAlchemyColumnType.TEXT
            else -> SqlAlchemyColumnType.STRING
        }
    }

    /**
     * Converts a SQL DEFAULT token into a Python expression. Only values we can safely express in
     * Python are kept; functions/keywords like CURRENT_TIMESTAMP are dropped so the generated
     * default never produces invalid Python.
     */
    private fun mapDefault(raw: String): String {
        val v = raw.trim()
        return when {
            v.equals("NULL", true) -> ""
            v.equals("TRUE", true) -> "True"
            v.equals("FALSE", true) -> "False"
            v.matches(NUMBER) -> v
            v.length >= 2 && v.startsWith("'") && v.endsWith("'") -> v // single-quoted: valid Python string
            else -> ""
        }
    }

    private fun unquote(s: String): String = s.trim().trim('`', '"', '[', ']').trim()

    private fun unquoteString(raw: String): String {
        val v = raw.trim()
        return if (v.length >= 2 && v.startsWith("'") && v.endsWith("'")) {
            v.substring(1, v.length - 1).replace("''", "'")
        } else v
    }

    private fun firstLine(message: String?): String =
        (message ?: "Invalid SQL").lineSequence().firstOrNull()?.trim()?.take(200) ?: "Invalid SQL"

    private val NUMBER = Regex("[-+]?\\d+(\\.\\d+)?")
}

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
    data class Failure(
        val message: String,
        /** 1-based caret position of the problem, or -1 when unknown. */
        val line: Int = -1,
        val column: Int = -1
    ) : SqlParseResult
}

/**
 * Parses CREATE TABLE DDL into a [ParsedTable] using the bundled JSqlParser, so SQL mode
 * works on every IDE regardless of whether the (paid) Database Tools and SQL plugin is present.
 *
 * Only the first CREATE TABLE statement is used (one model maps to one table). Foreign keys are
 * ignored for now — relationships are a separate, upcoming feature.
 */
object SqlDdlParser {

    fun parse(sql: String, dialect: SqlDialect = SqlDialect.GENERIC): SqlParseResult {
        if (sql.isBlank()) return SqlParseResult.Empty

        val createTable = try {
            extractCreateTable(sql)
        } catch (e: Throwable) {
            return failureFrom(e)
        } ?: return SqlParseResult.Failure("No CREATE TABLE statement found — paste a CREATE TABLE … statement.")

        return try {
            SqlParseResult.Success(buildTable(createTable, dialect))
        } catch (e: Throwable) {
            SqlParseResult.Failure(e.message ?: "Could not read the table definition.")
        }
    }

    /** Turns a raw parser exception into a human-friendly message and a caret position (if any). */
    private fun failureFrom(error: Throwable): SqlParseResult.Failure {
        val raw = error.message ?: error.cause?.message ?: ""
        val (line, column) = extractPosition(raw)
        return SqlParseResult.Failure(friendlyMessage(raw, line, column), line, column)
    }

    private fun extractPosition(raw: String): Pair<Int, Int> {
        val match = POSITION.find(raw) ?: return -1 to -1
        return (match.groupValues[1].toIntOrNull() ?: -1) to (match.groupValues[2].toIntOrNull() ?: -1)
    }

    private fun friendlyMessage(raw: String, line: Int, column: Int): String {
        val where = if (line >= 1 && column >= 1) " (line $line, column $column)" else ""
        val token = ENCOUNTERED.find(raw)?.groupValues?.get(1)?.trim()
        return when {
            token != null && (token.isEmpty() || token.equals("<EOF>", true)) ->
                "The statement looks incomplete$where — check for a missing comma, type or closing parenthesis."
            token != null ->
                "Unexpected “$token”$where — check the SQL syntax."
            raw.contains("Lexical error", ignoreCase = true) ->
                "Couldn’t read the SQL$where — check for an unclosed quote or comment."
            else ->
                "Couldn’t parse the SQL$where — check the CREATE TABLE syntax."
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

    private fun buildTable(ct: CreateTable, dialect: SqlDialect): ParsedTable {
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
                type = mapType(def.colDataType?.dataType ?: "", def.colDataType?.argumentsStringList, dialect),
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
    private fun mapType(rawType: String, args: List<String>?, dialect: SqlDialect): SqlAlchemyColumnType {
        val t = rawType.uppercase().substringBefore('(').trim()
        val singleArg = args?.singleOrNull()
        return when {
            // MySQL/MariaDB has no native boolean; TINYINT(1) is the BOOL/BOOLEAN convention.
            t == "TINYINT" && singleArg == "1" && dialect in setOf(SqlDialect.MYSQL, SqlDialect.MARIADB) ->
                SqlAlchemyColumnType.BOOLEAN
            // Oracle has no native boolean; NUMBER(1) is the common flag convention.
            t == "NUMBER" && singleArg == "1" && dialect == SqlDialect.ORACLE -> SqlAlchemyColumnType.BOOLEAN
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

    private val NUMBER = Regex("[-+]?\\d+(\\.\\d+)?")
    // JavaCC reports problems as "... at line 2, column 5." and "Encountered \"FOO\"".
    private val POSITION = Regex("line\\s+(\\d+),\\s*column\\s+(\\d+)", RegexOption.IGNORE_CASE)
    private val ENCOUNTERED = Regex("Encountered(?:\\s+unexpected\\s+token)?:?\\s+[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
}

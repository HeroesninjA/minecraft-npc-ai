package ro.ainpc.database

import java.sql.SQLException

enum class DatabaseDialect(val label: String) {
    SQLITE("sqlite"),
    MYSQL("mysql");

    companion object {
        fun fromConfig(value: String?): DatabaseDialect =
            when (value?.trim()?.lowercase()) {
                null, "", "sqlite" -> SQLITE
                "mysql", "mariadb" -> MYSQL
                else -> throw SQLException(
                    "Tip database necunoscut: '$value'. Valori acceptate: sqlite, mysql."
                )
            }
    }
}

object DatabaseDialectSql {
    fun autoIncrementPrimaryKey(dialect: DatabaseDialect): String =
        when (dialect) {
            DatabaseDialect.SQLITE -> "INTEGER PRIMARY KEY AUTOINCREMENT"
            DatabaseDialect.MYSQL -> "INTEGER PRIMARY KEY AUTO_INCREMENT"
        }

    fun shortText(dialect: DatabaseDialect, length: Int = 191): String =
        when (dialect) {
            DatabaseDialect.SQLITE -> "TEXT"
            DatabaseDialect.MYSQL -> "VARCHAR(${length.coerceIn(1, 1000)})"
        }

    fun longText(dialect: DatabaseDialect): String =
        when (dialect) {
            DatabaseDialect.SQLITE -> "TEXT"
            DatabaseDialect.MYSQL -> "LONGTEXT"
        }

    fun translateDml(sql: String, dialect: DatabaseDialect): String {
        if (dialect != DatabaseDialect.MYSQL) {
            return sql
        }
        return translateSqliteUpsert(sql)
            .let(::translateSqliteDateTime)
            .let(::translateSqliteLimitMax)
            .let(::translateSqliteScalarMinMax)
            .replace(Regex("\\bINSERT\\s+OR\\s+IGNORE\\s+INTO\\b", RegexOption.IGNORE_CASE), "INSERT IGNORE INTO")
            .replace(Regex("\\bINSERT\\s+OR\\s+REPLACE\\s+INTO\\b", RegexOption.IGNORE_CASE), "REPLACE INTO")
    }

    fun translateSchema(sql: String, dialect: DatabaseDialect): String =
        if (dialect == DatabaseDialect.MYSQL) {
            sql.replace(
                Regex("""(?i)\bCREATE\s+(UNIQUE\s+)?INDEX\s+IF\s+NOT\s+EXISTS\b""")
            ) { match ->
                val unique = match.groups[1]?.value.orEmpty()
                "CREATE ${unique}INDEX"
            }
        } else {
            sql
        }

    private fun translateSqliteUpsert(sql: String): String {
        val pattern = Regex(
            """(?is)\bON\s+CONFLICT\s*\([^)]*\)\s+DO\s+UPDATE\s+SET\s+(.+?)\s*$"""
        )
        val match = pattern.find(sql) ?: return sql
        val updateClause = match.groupValues[1]
            .replace(Regex("""(?i)\bexcluded\.([A-Za-z_][A-Za-z0-9_]*)"""), "VALUES($1)")
        val prefix = sql.substring(0, match.range.first).trimEnd()
        val separator = if (prefix.isBlank()) {
            "ON DUPLICATE KEY UPDATE\n"
        } else {
            "\nON DUPLICATE KEY UPDATE\n"
        }
        return prefix + separator + updateClause.trim()
    }

    private fun translateSqliteDateTime(sql: String): String =
        sql
            .replace(
                Regex("""(?i)datetime\s*\(\s*'now'\s*,\s*'\+'\s*\|\|\s*\?\s*\|\|\s*'\s*days'\s*\)"""),
                "DATE_ADD(UTC_TIMESTAMP(), INTERVAL ? DAY)"
            )
            .replace(
                Regex("""(?i)datetime\s*\(\s*'now'\s*,\s*'\+(\d+)\s+days'\s*\)"""),
                "DATE_ADD(UTC_TIMESTAMP(), INTERVAL $1 DAY)"
            )
            .replace(
                Regex("""(?i)datetime\s*\(\s*'now'\s*\)"""),
                "UTC_TIMESTAMP()"
            )

    private fun translateSqliteLimitMax(sql: String): String =
        sql.replace(
            Regex("""(?is)\bLIMIT\s+MAX\s*\(\s*0\s*,\s*\((.*?)\)\s*-\s*\?\s*\)""")
        ) { match ->
            "LIMIT GREATEST(0, (${match.groupValues[1].trim()}) - ?)"
        }

    private fun translateSqliteScalarMinMax(sql: String): String {
        val argument = """(?:[A-Za-z0-9_.+\- ?]+|[A-Za-z_][A-Za-z0-9_]*\([^()]*\))"""
        val maxPattern = Regex("""(?i)\bMAX\s*\(\s*($argument)\s*,\s*($argument)\s*\)""")
        val minPattern = Regex("""(?i)\bMIN\s*\(\s*($argument)\s*,\s*($argument)\s*\)""")
        var result = sql
        repeat(4) {
            val before = result
            result = result
                .replace(maxPattern, "GREATEST($1, $2)")
                .replace(minPattern, "LEAST($1, $2)")
            if (result == before) {
                return result
            }
        }
        return result
    }
}

package com.noisebomb.sqlalchemy.util

object NamingUtils {

    fun toSnakeCase(input: String): String {
        return input
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .lowercase()
    }

    fun toCamelCase(input: String): String {
        return input.split("_")
            .filter { it.isNotEmpty() }
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}

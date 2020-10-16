/*
 * Copyright 2018 DoorBuzzin, Q-Jam B.V.
 */
package com.qjam.c.foundations.db.exposed

import com.qjam.c.foundations.Identifier
import org.jetbrains.exposed.sql.ColumnType

open class IdentifierColumnType : ColumnType() {
    override fun sqlType(): String = buildString {
        append("VARCHAR(255)")
    }

    override fun nonNullValueToString(value: Any): String = buildString {
        val valueAsString = when (value) {
            is Identifier<*>, is String -> valueToIdentifier(value).toString()
            else -> error("Unexpected value of type Identifier: ${value.javaClass.canonicalName}")
        }
        append('\'')
        append(valueAsString)
        append('\'')
    }

    override fun notNullValueToDB(value: Any): Any = valueToIdentifier(value).toString()

    private fun valueToIdentifier(value: Any): Identifier<*> =
        when (value) {
            is Identifier<*> -> value
            is String -> Identifier.of<Identifier.Tag>(value)
            else -> error("Unexpected value of type Identifier: ${value.javaClass.canonicalName}")
        }

    override fun valueFromDB(value: Any): Any =
        when (value) {
            is Identifier<*> -> value
            is String -> Identifier.of<Identifier.Tag>(value)
            else -> error("Unexpected value of type Identifier: $value of ${value::class.qualifiedName}")
        }
}

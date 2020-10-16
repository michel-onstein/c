/*
 * Copyright 2018 DoorBuzzin, Q-Jam B.V.
 */
package com.qjam.c.foundations.db.exposed

import com.qjam.c.foundations.Identifier
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.sql.Column


abstract class IdentifierIdTable<I : Identifier.Tag>(name: String = "", columnName: String = "id") : EnhancedIdTable<Identifier<I>>(name) {
    override val id: Column<EntityID<Identifier<I>>> = identifier(columnName).primaryKey().clientDefault { generateIdentifier() }.entityId()

    /**
     * An string column to store an identifier as base64 in
     *
     * @param name The column name
     */
    fun identifier(name: String): Column<Identifier<I>> = registerColumn(name, IdentifierColumnType())

    /**
     * Generate a new identifier for this table
     *
     * @return newly created identifier
     */
    abstract fun generateIdentifier(): Identifier<I>
}

abstract class IdentifierEntity<I : Identifier.Tag>(id: EntityID<Identifier<I>>) : Entity<Identifier<I>>(id)

abstract class IdentifierEntityClass<I : Identifier.Tag, out E : IdentifierEntity<I>>(table: IdTable<Identifier<I>>, entityType: Class<E>? = null) : EntityClass<Identifier<I>, E>(table, entityType)

/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.db

import com.qjam.c.foundations.Identifier
import com.qjam.c.foundations.db.exposed.IdentifierEntity
import com.qjam.c.foundations.db.exposed.IdentifierEntityClass
import com.qjam.c.foundations.db.exposed.IdentifierIdTable
import org.jetbrains.exposed.dao.EntityID

object Enterprises : IdentifierIdTable<com.qjam.c.model.Enterprise.Tag>() {
    override fun generateIdentifier(): Identifier<com.qjam.c.model.Enterprise.Tag> = Identifier.random()

    val name = varchar("name", 256).uniqueIndex()
}


class Enterprise(id: EntityID<Identifier<com.qjam.c.model.Enterprise.Tag>>) :
    IdentifierEntity<com.qjam.c.model.Enterprise.Tag>(id) {
    companion object : IdentifierEntityClass<com.qjam.c.model.Enterprise.Tag, Enterprise>(Enterprises) {

    }

    var name by Enterprises.name

    /**
     * @return model instance
     */
    fun model(): com.qjam.c.model.Enterprise {
        return com.qjam.c.model.Enterprise(
            id.value,
            name,
        )
    }
}



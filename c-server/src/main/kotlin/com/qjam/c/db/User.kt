/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.db

import com.qjam.c.foundations.Identifier
import com.qjam.c.foundations.db.exposed.IdentifierEntity
import com.qjam.c.foundations.db.exposed.IdentifierEntityClass
import com.qjam.c.foundations.db.exposed.IdentifierIdTable
import org.jetbrains.exposed.dao.EntityID

object Users : IdentifierIdTable<com.qjam.c.model.User.Tag>() {
    override fun generateIdentifier(): Identifier<com.qjam.c.model.User.Tag> = Identifier.random()

    val enterprise = Users.reference("enterprise", Enterprises).index()
    val name = varchar("name", 256)
    val email = varchar("email", 256)
}


class User(id: EntityID<Identifier<com.qjam.c.model.User.Tag>>) :
    IdentifierEntity<com.qjam.c.model.User.Tag>(id) {
    companion object : IdentifierEntityClass<com.qjam.c.model.User.Tag, User>(Users) {

    }

    var enterprise by Enterprise referencedOn Users.enterprise
    var name by Users.name
    var email by Users.email

    /**
     * @return model instance
     */
    fun model(): com.qjam.c.model.User {
        return com.qjam.c.model.User(
            id.value,
            name,
            email,
        )
    }
}



/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object UserTokens : IntIdTable() {
    val user = reference("user", Users)
    val token = varchar("token", 256).uniqueIndex()
    val expires = long("expires")
}


class UserToken(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserToken>(UserTokens)

    var user by User referencedOn UserTokens.user
    var token by UserTokens.token
    var expires by UserTokens.expires
}



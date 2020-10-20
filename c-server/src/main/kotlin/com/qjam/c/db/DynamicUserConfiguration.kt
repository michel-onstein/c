/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object DynamicUserConfigurations : IntIdTable() {
    val user = reference("user", Users)
    val key = varchar("key", 256).index()
    val value = varchar("value", 256)

    init {
        index(true, user, key)
    }
}


class DynamicUserConfiguration(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DynamicUserConfiguration>(DynamicUserConfigurations)

    var user by User referencedOn DynamicUserConfigurations.user
    var key by DynamicUserConfigurations.key
    var value by DynamicUserConfigurations.value
}



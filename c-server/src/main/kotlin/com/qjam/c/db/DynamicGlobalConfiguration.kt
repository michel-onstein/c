/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object DynamicGlobalConfigurations : IntIdTable() {
    val enterprise = reference("enterprise", Enterprises)
    val key = varchar("key", 256).index()
    val value = varchar("value", 256)

    init {
        index(true, enterprise,key)
    }
}


class DynamicGlobalConfiguration(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DynamicGlobalConfiguration>(DynamicGlobalConfigurations)

    var enterprise by Enterprise referencedOn DynamicGlobalConfigurations.enterprise
    var key by DynamicGlobalConfigurations.key
    var value by DynamicGlobalConfigurations.value
}



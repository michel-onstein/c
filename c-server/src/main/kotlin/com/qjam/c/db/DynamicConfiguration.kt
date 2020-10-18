/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object DynamicConfigurations : IntIdTable() {
    val enterprise = reference("enterprise", Enterprises)
    val key = varchar("key", 256).index()
    val value = varchar("value", 256)

    init {
        index(true, enterprise,key)
    }
}


class DynamicConfiguration(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DynamicConfiguration>(DynamicConfigurations)

    var enterprise by Enterprise referencedOn DynamicConfigurations.enterprise
    var key by DynamicConfigurations.key
    var value by DynamicConfigurations.value
}



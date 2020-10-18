/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Hosts : IntIdTable() {
    val enterprise = reference("enterprise", Enterprises).index()

    val name = varchar("name", 256).index()
}


class Host(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Host>(Hosts)

    var enterprise by Enterprise referencedOn Hosts.enterprise

    var name by Hosts.name
}



/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Containers : IntIdTable() {
    val host = reference("host", Hosts).index()

    val type = varchar("type", 256).index()
    val name = varchar("name", 256).index()
    val image = varchar("image", 256).index()
}


class Container(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Container>(Containers)

    var host by Host referencedOn Containers.host

    var type by Containers.type
    var name by Containers.name
    var image by Containers.image
}



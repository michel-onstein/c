/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Packages : IntIdTable() {
    val container = reference("container", Containers).index()

    val name = varchar("name", 256).index()
    val version = varchar("version", 256).index()
    val manager = varchar("manager", 256).index()

    val time = long("time").index()
}


class Package(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Package>(Packages)

    var container by Container referencedOn Packages.container

    var name by Packages.name
    var version by Packages.version
    var manager by Packages.manager

    var time by Packages.time
}



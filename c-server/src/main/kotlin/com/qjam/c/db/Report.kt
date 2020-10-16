/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Reports : IntIdTable() {
    val enterprise = reference("enterprise", Enterprises)

    val uuid = varchar("uuid", 256).uniqueIndex()
    val hostname = varchar("hostname", 256).index()
    val time = long("time")
}


class Report(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Report>(Reports)

    var enterprise by Enterprise referencedOn Reports.enterprise

    var uuid by Reports.uuid
    var hostname by Reports.hostname
    var time by Reports.time
}



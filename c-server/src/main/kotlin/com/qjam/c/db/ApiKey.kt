/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object ApiKeys : IntIdTable() {
    val enterprise = reference("enterprise", Enterprises)
    val apiKey = varchar("key", 256).uniqueIndex()
}


class ApiKey(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ApiKey>(ApiKeys)

    var enterprise by Enterprise referencedOn ApiKeys.enterprise
    var apiKey by ApiKeys.apiKey
}



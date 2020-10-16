/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object EnterpriseRealms : IntIdTable() {
    val enterprise = reference("enterprise", Enterprises)
    val realm = varchar("key", 256).uniqueIndex()
    val type = char("type")
}


class EnterpriseRealm(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EnterpriseRealm>(EnterpriseRealms)

    var enterprise by Enterprise referencedOn EnterpriseRealms.enterprise
    var realm by EnterpriseRealms.realm
    var type by EnterpriseRealms.type
}



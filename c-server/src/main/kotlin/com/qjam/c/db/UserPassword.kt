/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.db

import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object UserPasswords : IntIdTable() {
    val user = reference("user", Users)
    val hash = varchar("hash", 256)
    val seed = varchar("seed", 256)
}


class UserPassword(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserPassword>(UserPasswords) {
        fun hashPassword(seed: String, password: String): String {
            return BaseEncoding.base64Url()
                .encode(Hashing.sha256().hashString(seed + password + seed, Charsets.UTF_8).asBytes())
                .toString()
        }
    }

    var user by User referencedOn UserPasswords.user
    var hash by UserPasswords.hash
    var seed by UserPasswords.seed

    var password: String
        get() = throw RuntimeException("Cannot determine password from hash")
        set(value) {
            // Build random seed
            val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            val seed = (1..8)
                .map { _ -> kotlin.random.Random.nextInt(0, charPool.size) }
                .map(charPool::get)
                .joinToString("")

            hash = hashPassword(seed, value)
            this.seed = seed
        }
}



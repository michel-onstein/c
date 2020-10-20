/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.api.v1

import com.google.common.flogger.FluentLogger
import com.qjam.c.UserAttributeKey
import com.qjam.c.db.DynamicUserConfiguration
import com.qjam.c.db.DynamicUserConfigurations
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.KoinContextHandler

class ApiV1Configuration {
    companion object {
        val koin = KoinContextHandler.get()
        val logger = FluentLogger.forEnclosingClass()

        fun install(application: Application) {
            application.routing {
                /**
                 * Get the user configuration
                 */
                get("/api/v1/configuration") {
                    val user = call.attributes.getOrNull(UserAttributeKey)
                    if (user == null) {
                        call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                        return@get
                    }

                    val entries = mutableListOf<ConfigurationResponseEntry>()
                    transaction {
                        val userConfigurations =
                            DynamicUserConfiguration.find(DynamicUserConfigurations.user eq user.id)
                        userConfigurations.forEach {
                            entries.add(ConfigurationResponseEntry(it.key, it.value))
                        }
                    }

                    call.respondJson(Json.encodeToString(entries))
                }
            }
        }
    }
}

@Serializable
data class ConfigurationResponseEntry(val key: String, val value: String)

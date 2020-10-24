/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.api.v1

import com.google.common.flogger.FluentLogger
import com.qjam.c.EnterpriseAttributeKey
import com.qjam.c.db.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.KoinContextHandler

class ApiV1GlobalConfiguration {
    companion object {
        val koin = KoinContextHandler.get()
        val logger = FluentLogger.forEnclosingClass()

        fun install(application: Application) {
            application.routing {
                /**
                 * Get the user configuration
                 */
                get("/api/v1/global-configuration") {
                    val enterprise = call.attributes.getOrNull(EnterpriseAttributeKey)
                    if (enterprise == null) {
                        call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                        return@get
                    }

                    // TODO Reimplement, just output as json map
                    val entries = mutableListOf<GlobalConfigurationEntry>()
                    transaction {
                        val userConfigurations =
                            DynamicGlobalConfiguration.find(DynamicGlobalConfigurations.enterprise eq enterprise.id)
                        userConfigurations.forEach {
                            entries.add(GlobalConfigurationEntry(it.key, it.value))
                        }
                    }

                    call.respondJson(Json.encodeToString(entries))
                }

                /*
                 * Set a user configuration entry
                 */
                put("/api/v1/global-configuration") {
                    if (!ensureJsonContent(call)) {
                        ApiV1Login.logger.atWarning()
                            .log(
                                "Received non-json request from %s",
                                call.request.local.remoteHost
                            )
                        call.respondText("Bad Request", status = HttpStatusCode.BadRequest)
                        return@put
                    }

                    val enterprise = call.attributes.getOrNull(EnterpriseAttributeKey)
                    if (enterprise == null) {
                        call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                        return@put
                    }

                    val request = try {
                        Json.decodeFromString<GlobalConfigurationEntry>(call.receiveText())
                    } catch (t: Throwable) {
                        call.respondText("Bad Request", status = HttpStatusCode.BadRequest)
                        return@put
                    }

                    // Some sanity here
                    val key = sanitizeKey(request.key)
                    val value = request.value

                    if (key == null) {
                        call.respondText("Bad Request", status = HttpStatusCode.BadRequest)
                        return@put
                    }

                    transaction {
                        // TODO If only we had upsert
                        val entries =
                            DynamicGlobalConfiguration.find { (DynamicGlobalConfigurations.key eq key) and (DynamicGlobalConfigurations.enterprise eq enterprise.id) }
                        if (!entries.empty()) {
                            val entry = entries.first()
                            entry.value = value
                        } else {
                            val dbEnterprise = Enterprise[enterprise.id]
                            val entry = DynamicGlobalConfiguration.new {
                                this.enterprise = dbEnterprise
                                this.key = key
                                this.value = value
                            }
                        }
                    }

                    call.respondText("OK")
                }

                /*
                 * Delete a user configuration entry
                 */
                delete("/api/v1/global-configuration/{key}") {
                    val enterprise = call.attributes.getOrNull(EnterpriseAttributeKey)
                    if (enterprise == null) {
                        call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                        return@delete
                    }

                    val key = sanitizeKey(call.parameters["key"])
                    if (key == null) {
                        call.respondText("Bad Request", status = HttpStatusCode.BadRequest)
                        return@delete
                    }

                    val found = transaction {
                        val entries =
                            DynamicGlobalConfiguration.find { (DynamicGlobalConfigurations.key eq key) and (DynamicGlobalConfigurations.enterprise eq enterprise.id) }
                        if (entries.empty()) {
                            false
                        } else {
                            entries.first().delete()
                            true
                        }
                    }

                    if (!found) {
                        call.respondText("Not Found", status = HttpStatusCode.NotFound)
                        return@delete
                    }

                    call.respondText("OK")
                }

                /*
                 * Get a user configuration entry
                 */
                get("/api/v1/global-configuration/{key}") {
                    val enterprise = call.attributes.getOrNull(EnterpriseAttributeKey)
                    if (enterprise == null) {
                        call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                        return@get
                    }

                    val key = sanitizeKey(call.parameters["key"])
                    if (key == null) {
                        call.respondText("Bad Request", status = HttpStatusCode.BadRequest)
                        return@get
                    }

                    val value = transaction {
                        val entries =
                            DynamicGlobalConfiguration.find { (DynamicGlobalConfigurations.key eq key) and (DynamicGlobalConfigurations.enterprise eq enterprise.id) }
                        if (entries.empty()) {
                            null
                        } else {
                            entries.first().value
                        }
                    }

                    if (value == null) {
                        call.respondText("Not Found", status = HttpStatusCode.NotFound)
                        return@get
                    }

                    call.respondJson(Json.encodeToString(value))
                }
            }
        }
    }
}

private fun sanitizeKey(key: String?): String? {
    if (key == null) {
        return null
    }

    if (key.length > 255) {
        return null
    }

    // TODO Check for invalid key characters
    return key.toLowerCase()
}

@Serializable
data class GlobalConfigurationEntry(val key: String, val value: String)

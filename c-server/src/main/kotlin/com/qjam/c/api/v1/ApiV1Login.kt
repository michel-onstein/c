/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.api.v1

import com.google.common.flogger.FluentLogger
import com.qjam.c.db.EnterpriseRealm
import com.qjam.c.db.EnterpriseRealms
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.KoinContextHandler

class ApiV1Login {
    companion object {
        val koin = KoinContextHandler.get()
        val logger = FluentLogger.forEnclosingClass()

        fun install(application: Application) {
            application.routing {
                post("/api/v1/login") {
                    if (!ensureJsonContent(call)) {
                        logger.atWarning()
                            .log(
                                "Received non-json request from %s",
                                call.request.local.remoteHost
                            )
                        call.respondText("Bad Request", status = HttpStatusCode.BadRequest)
                        return@post
                    }

                    val request = try {
                        Json.decodeFromString<LoginRequest>(call.receiveText())
                    } catch (t: Throwable) {
                        call.respondText("Bad Request", status = HttpStatusCode.BadRequest)
                        return@post
                    }

                    // Figure out the realm and thus the login type
                    val parts = request.username.split("@")
                    if (parts.size != 2) {
                        logger.atFine().log(
                            "Malformed username '%s' received from %s",
                            request.username,
                            call.request.local.remoteHost
                        )
                        call.respondText("Bad Request", status = HttpStatusCode.BadRequest)
                        return@post
                    }

                    val type = transaction {
                        val realms = EnterpriseRealm.find { EnterpriseRealms.realm eq parts[1] }
                        if (realms.empty()) {
                            null
                        } else {
                            realms.first().type
                        }
                    }

                    // Handle the different types
                    when (type) {
                        null -> {
                            // Unknown realm, pretend it exists as plain
                            logger.atFine().log(
                                "Received login request for unknown realm '%s' from %s",
                                parts[1],
                                call.request.local.remoteHost
                            )
                            call.respondText(
                                Json.encodeToString(LoginResponse.serializer(), LoginResponse('p')),
                                ContentType.Application.Json,
                                HttpStatusCode.OK
                            )
                        }
                        'p' -> {
                            // plain login
                            call.respondText(
                                Json.encodeToString(LoginResponse.serializer(), LoginResponse('p')),
                                ContentType.Application.Json,
                                HttpStatusCode.OK
                            )
                        }
                        else -> throw NotImplementedError()
                    }
                }
            }
        }
    }
}

@Serializable
class LoginRequest(val username: String)

@Serializable
class LoginResponse(val type: Char)

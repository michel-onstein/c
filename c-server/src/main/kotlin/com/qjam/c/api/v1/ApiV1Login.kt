/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.api.v1

import com.google.common.flogger.FluentLogger
import com.google.common.io.BaseEncoding
import com.qjam.c.config.AuthenticationConfiguration
import com.qjam.c.db.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.KoinContextHandler
import java.util.*
import kotlin.random.Random

class ApiV1Login {
    companion object {
        val koin = KoinContextHandler.get()
        val logger = FluentLogger.forEnclosingClass()

        fun install(application: Application) {
            application.routing {
                /**
                 * Login info, the first stage in the login process used to determine what kind of login needs to be
                 * performed e.g., plain, oauth2, ...
                 */
                post("/api/v1/login-info") {
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
                        Json.decodeFromString<LoginInfoRequest>(call.receiveText())
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

                    val type = determineTypeFromRealm(parts[1])

                    // Handle the different types
                    when (type) {
                        null -> {
                            // Unknown realm, pretend it exists as plain
                            logger.atFine().log(
                                "Received login-info request for unknown realm '%s' from %s",
                                parts[1],
                                call.request.local.remoteHost
                            )
                            call.respondText(
                                Json.encodeToString(LoginInfoResponse.serializer(), LoginInfoResponse('p')),
                                ContentType.Application.Json,
                                HttpStatusCode.OK
                            )
                        }
                        'p' -> {
                            // plain login
                            call.respondText(
                                Json.encodeToString(LoginInfoResponse.serializer(), LoginInfoResponse('p')),
                                ContentType.Application.Json,
                                HttpStatusCode.OK
                            )
                        }
                        else -> throw NotImplementedError()
                    }
                }

                post("/api/v1/plain-login") {
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
                        Json.decodeFromString<PlainLoginRequest>(call.receiveText())
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

                    val type = determineTypeFromRealm(parts[1])

                    // Whatever we do, do it slow, prevent brute force
                    delay(1000)

                    if (type != 'p') {
                        // Either an unknown login attempt or someone trying to sneak past oauth etc.
                        call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                        return@post
                    }

                    val userId = transaction {
                        val result =
                            (Users innerJoin UserPasswords).slice(Users.id, UserPasswords.seed, UserPasswords.hash)
                                .select(Users.email eq request.username).first()

                        val hashOfProvidedPassword =
                            UserPassword.hashPassword(result[UserPasswords.seed], request.password)
                        if (hashOfProvidedPassword != result[UserPasswords.hash]) {
                            null
                        } else {
                            result[Users.id].value
                        }
                    }

                    if (userId == null) {
                        // Hash didnt match, no can do
                        call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                        return@post
                    }

                    // Get the user model instance for serialization
                    val user = transaction {
                        User[userId].model()
                    }

                    // Build a token
                    val token = BaseEncoding.base64Url().encode(Random.nextBytes(64)).toString()

                    // The token will expire if not used
                    val expires =
                        Date().time + (koin.get<AuthenticationConfiguration>().authenticationUserTokenTTL * 1000L)

                    // Save new user token
                    transaction {
                        UserToken.new {
                            this.user = User[userId]
                            this.token = token
                            this.expires = expires
                        }
                    }

                    // Respond back to the client
                    call.respondText(
                        Json.encodeToString(
                            LoginResponse.serializer(), LoginResponse(
                                token,
                                user,
                            )
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.OK
                    )
                }
            }
        }

        fun determineTypeFromRealm(realm: String): Char? {
            return transaction {
                val realms = EnterpriseRealm.find { EnterpriseRealms.realm eq realm }
                if (realms.empty()) {
                    null
                } else {
                    realms.first().type
                }
            }


        }

    }
}

@Serializable
class LoginInfoRequest(val username: String)

@Serializable
class LoginInfoResponse(val type: Char)

@Serializable
class PlainLoginRequest(val username: String, val password: String)

@Serializable
class LoginResponse(
    val token: String,
    val user: com.qjam.c.model.User,
)

/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c

import com.google.common.flogger.FluentLogger
import com.qjam.c.api.v1.ApiV1
import com.qjam.c.config.AllConfiguration
import com.qjam.c.config.AuthenticationConfiguration
import com.qjam.c.config.DatabaseConnectionConfiguration
import com.qjam.c.db.ApiKey
import com.qjam.c.db.ApiKeys
import com.qjam.c.db.UserToken
import com.qjam.c.db.UserTokens
import com.qjam.c.impl.DevelopmentConfigurationImpl
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.request.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.websocket.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.KoinContextHandler
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.util.*

val configurationKoinModule = module {
    single { DevelopmentConfigurationImpl() as AllConfiguration }
    single { get<AllConfiguration>() as AuthenticationConfiguration }
    single { get<AllConfiguration>() as DatabaseConnectionConfiguration }
}

val logger = FluentLogger.forEnclosingClass();

val EnterpriseAttributeKey = AttributeKey<com.qjam.c.model.Enterprise>("Enterprise")
val UserAttributeKey = AttributeKey<com.qjam.c.model.User>("User")

fun main() {
    System.setProperty(
        "flogger.backend_factory", "com.google.common.flogger.backend.log4j.Log4jBackendFactory#getInstance"
    )

    logger.atInfo()
        .log("A Server version '${Version.version}', built from git-revision '${Version.revision}', by ${Version.by} at ${Version.time}")

    startKoin {
        modules(
            configurationKoinModule,
        )
    }

    val koin = KoinContextHandler.get()

    // Setup the database
    DatabaseHelper.initializeDatabase(koin.get(), true)

    // Setup ktor
    val server = embeddedServer(Netty, 1080) {
        install(CallLogging)
        install(WebSockets)

        // Determine the active enterprise based on api-key
        intercept(ApplicationCallPipeline.Features) {
            authorizeWithApiKey(call)
            authorizeWithToken(call)
        }
        ApiV1.install(this)
    }

    server.start(true)
}

private fun authorizeWithApiKey(call: ApplicationCall) {
    val key = call.request.header("X-API-KEY") ?: return

    val enterprise = transaction {
        val apiKeys = ApiKey.find { ApiKeys.apiKey eq key }.limit(1)

        if (apiKeys.empty()) {
            null
        } else {
            val apiKey = apiKeys.first()

            if ((apiKey.expires != 0L) && (apiKey.expires < Date().time)) {
                null
            } else {
                apiKey.enterprise.model()
            }
        }
    }

    if (enterprise != null) {
        call.attributes.put(EnterpriseAttributeKey, enterprise)
    }

}

private fun authorizeWithToken(call: ApplicationCall) {
    val bearer = call.request.header("Authorization") ?: return

    val parts = bearer.split(" ")
    if (parts.size != 2) {
        logger.atFiner().log("Trying to authorize with malformed bearer: %s", bearer)
        return
    }
    val token = parts[1]
    transaction {
        val userTokens = UserToken.find { UserTokens.token eq token }
        if (userTokens.empty()) {
            logger.atFiner().log("Trying to authorize with unknown token: %s", bearer)
        } else {
            val userToken = userTokens.first()
            if (userToken.expires < Date().time) {
                userToken.delete()
                return@transaction
            }

            val user = userToken.user
            val userModel = user.model()
            val enterpriseModel = user.enterprise.model()

            call.attributes.put(UserAttributeKey, userModel)
            call.attributes.put(EnterpriseAttributeKey, enterpriseModel)

        }
    }
}

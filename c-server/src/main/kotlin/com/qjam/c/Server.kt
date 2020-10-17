/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c

import com.google.common.flogger.FluentLogger
import com.qjam.c.api.v1.ApiV1
import com.qjam.c.config.AllConfiguration
import com.qjam.c.config.DatabaseConnectionConfiguration
import com.qjam.c.db.*
import com.qjam.c.impl.DevelopmentConfigurationImpl
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.request.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.websocket.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.KoinContextHandler
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.util.*

val configurationKoinModule = module {
    single { DevelopmentConfigurationImpl() as AllConfiguration }
    single { get<AllConfiguration>() as DatabaseConnectionConfiguration }
}

val logger = FluentLogger.forEnclosingClass();

val EnterpriseAttributeKey = AttributeKey<com.qjam.c.model.Enterprise>("Enterprise")

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
    initializeDatabase(koin.get(), true)

    // Setup ktor
    val server = embeddedServer(Netty, 1080) {
        install(CallLogging)
        install(WebSockets)

        // Determine the active enterprise based on api-key
        intercept(ApplicationCallPipeline.Features) {
            val key = call.request.header("X-API-KEY")
            if (key != null) {
                val enterprise = transaction {
                    val apiKeys = ApiKey.find { ApiKeys.apiKey eq key }.limit(1)

                    if (apiKeys.empty()) {
                        null
                    } else {
                        val apiKey = apiKeys.first()

                        if (apiKey.expires < Date().time) {
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
        }
        ApiV1.install(this)
    }

    server.start(true)
}

fun initializeDatabase(databaseConnectionConfiguration: DatabaseConnectionConfiguration, fillWithTestData: Boolean) {
    with(databaseConnectionConfiguration) {
        if (this.databaseConnectionUser != null) {
            logger.atInfo()
                .log("Connecting to database with: path(${this.databaseConnectionUrl}), driver(${this.databaseConnectionDriver}), user(${this.databaseConnectionUser!!}), password(${this.databaseConnectionPassword!!}).")
            Database.connect(
                this.databaseConnectionUrl,
                this.databaseConnectionDriver,
                this.databaseConnectionUser!!,
                this.databaseConnectionPassword!!
            )
        } else {
            logger.atInfo()
                .log("Connecting to database with: path(${this.databaseConnectionUrl}), driver(${this.databaseConnectionDriver}).")
            Database.connect(this.databaseConnectionUrl, driver = this.databaseConnectionDriver)
        }
    }

    val allTables = arrayOf(
        ApiKeys,
        Enterprises,
        EnterpriseRealms,
        Reports,
        Users,
        UserPasswords,
        UserTokens,
    )

    transaction {
        SchemaUtils.create(*allTables)
    }

    if (!fillWithTestData) {
        return
    }

    transaction {
        val enterprise1 = Enterprise.new {
            name = "Enterprise 1"
        }

        val apiKey1 = ApiKey.new {
            apiKey = "howdy"
            enterprise = enterprise1
            expires = Date().time+(1000*60*60*24)
        }

        val realm1 = EnterpriseRealm.new {
            realm = "onstein.net"
            type = 'p'
            enterprise = enterprise1
        }

        val user1 = User.new {
            name = "Michel Onstein"
            email = "michel@onstein.net"
            enterprise = enterprise1
        }

        val user1Password = UserPassword.new {
            password = "testing123"
            user = user1
        }
    }


}


/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c

import com.qjam.c.config.DatabaseConnectionConfiguration
import com.qjam.c.db.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class DatabaseHelper {
    companion object {
        fun initializeDatabase(
            databaseConnectionConfiguration: DatabaseConnectionConfiguration,
            fillWithTestData: Boolean
        ) {
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
                Containers,
                DynamicGlobalConfigurations,
                DynamicUserConfigurations,
                Enterprises,
                EnterpriseRealms,
                Hosts,
                Packages,
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
                    expires = Date().time + (1000 * 60 * 60 * 24)
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

                val user2 = User.new {
                    name = "John Doe"
                    email = "john@onstein.net"
                    enterprise = enterprise1
                }

                val user1Password = UserPassword.new {
                    password = "testing123"
                    user = user1
                }

                val user2Password = UserPassword.new {
                    password = "testing123"
                    user = user2
                }

                val user1Configuration1 = DynamicUserConfiguration.new {
                    user = user1
                    key = "key1"
                    value = "value1"
                }

                val user1Configuration2 = DynamicUserConfiguration.new {
                    user = user1
                    key = "key2"
                    value = "value2"
                }
            }
        }
    }
}

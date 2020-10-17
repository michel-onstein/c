/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.impl

import com.qjam.c.config.AllConfiguration

class DevelopmentConfigurationImpl : AllConfiguration {
    override val authenticationUserTokenTTL = 3600*24

    override val databaseConnectionUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    override val databaseConnectionDriver = "org.h2.Driver"
    override val databaseConnectionUser: String? = null
    override val databaseConnectionPassword: String? = null
}

/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.config

interface DatabaseConnectionConfiguration {
    val databaseConnectionUrl: String
    val databaseConnectionDriver: String
    val databaseConnectionUser: String?
    val databaseConnectionPassword: String?
}

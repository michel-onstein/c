/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.config

interface AuthenticationConfiguration {
    val authenticationUserTokenTTL: Int // Time to live of the user token in seconds
}

/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.model

import com.qjam.c.foundations.Identifier
import com.qjam.c.foundations.IdentifierSerializer
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @Serializable(with = IdentifierSerializer::class)
    val id: Identifier<User.Tag>,

    val name: String,
    val email: String,
) {
    interface Tag : Identifier.Tag
}


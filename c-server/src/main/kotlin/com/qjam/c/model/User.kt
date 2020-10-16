/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.model

import com.qjam.c.foundations.Identifier

data class User(
    val id: Identifier<User.Tag>,

    val name: String,
    val email: String,
) {
    interface Tag : Identifier.Tag
}


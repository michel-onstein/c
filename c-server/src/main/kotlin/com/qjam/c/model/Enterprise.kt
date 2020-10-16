/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.model

import com.qjam.c.foundations.Identifier

data class Enterprise(
    val id: Identifier<Enterprise.Tag>,

    val name: String,
) {
    interface Tag : Identifier.Tag
}


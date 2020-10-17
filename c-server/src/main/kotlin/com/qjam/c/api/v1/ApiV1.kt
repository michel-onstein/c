/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.api.v1

import com.qjam.c.api.v1.report.ApiV1Report
import io.ktor.application.*
import io.ktor.request.*

class ApiV1 {
    companion object {
        fun install(application: Application) {
            ApiV1Login.install(application)
            ApiV1Report.install(application)
        }
    }
}

fun ensureJsonContent(call: ApplicationCall): Boolean {
    val key = call.request.header("Content-Type") ?: return false

    return key.toLowerCase() == "application/json"
}

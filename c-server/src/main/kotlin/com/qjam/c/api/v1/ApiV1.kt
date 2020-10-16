/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.api.v1

import com.qjam.c.api.v1.report.ApiV1Report
import io.ktor.application.*

class ApiV1 {
    companion object {
        fun install(application: Application) {
            ApiV1Report.install(application)
        }
    }
}

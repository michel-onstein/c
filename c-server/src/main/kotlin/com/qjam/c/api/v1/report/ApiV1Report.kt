/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.api.v1.report

import com.google.common.flogger.FluentLogger
import com.qjam.c.EnterpriseAttributeKey
import com.qjam.c.api.v1.ensureJsonContent
import com.qjam.c.api.v1.report.model.Report
import com.qjam.c.db.Enterprise
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.KoinContextHandler

class ApiV1Report {
    companion object {
        val koin = KoinContextHandler.get()
        val logger = FluentLogger.forEnclosingClass()

        fun install(application: Application) {
            application.routing {
                post("/api/v1/report") {
                    logger.atInfo().log("Report received from %s", call.request.local.remoteHost)
                    val enterprise = call.attributes.getOrNull(EnterpriseAttributeKey)
                    if (enterprise == null) {
                        logger.atWarning()
                            .log(
                                "Received report request from %s without (valid) api key",
                                call.request.local.remoteHost
                            )
                        call.respondText("Forbidden", status = HttpStatusCode.Forbidden)
                        return@post
                    }

                    if (!ensureJsonContent(call)) {
                        logger.atWarning()
                            .log(
                                "Received non-json report request from %s",
                                call.request.local.remoteHost
                            )
                        call.respondText("Bad Request", status = HttpStatusCode.BadRequest)
                        return@post
                    }

                    val reportAsJson = call.receiveText()
                    println(reportAsJson)

                    val report = Json.decodeFromString(Report.serializer(), reportAsJson)

                    // Store report
                    transaction {
                        val exposedEnterprise = Enterprise[enterprise.id]
                        val exposedReport = com.qjam.c.db.Report.new {
                            this.enterprise = exposedEnterprise
                            uuid = report.uuid
                            hostname = report.hostname
                            time = report.time
                        }
                    }

                    call.respondText("OK")
                }
            }
        }
    }
}




/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.api.v1.report

import com.google.common.flogger.FluentLogger
import com.qjam.c.EnterpriseAttributeKey
import com.qjam.c.api.v1.ensureJsonContent
import com.qjam.c.api.v1.report.model.Package
import com.qjam.c.api.v1.report.model.Report
import com.qjam.c.db.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
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

                    /*
                     * Store report
                     * TODO None of this is thread/multi instance safe
                     */
                    transaction {
                        val exposedEnterprise = Enterprise[enterprise.id]

                        // Try to find a matching host, if none can be found create one
                        val hostResult = Host.find(Hosts.name eq report.hostname)
                        val host = if (hostResult.empty()) {
                            Host.new {
                                this.enterprise = exposedEnterprise
                                this.name = report.hostname
                            }
                        } else {
                            hostResult.first()
                        }

                        // Create containers for the host
                        // A root container is needed
                        val rootContainerId = if ((report.packages != null) && (report.packages.isNotEmpty())) {
                            val containers =
                                Container.find((Containers.host eq host.id) and (Containers.type eq "root"))
                            val container = if (containers.empty()) {
                                Container.new {
                                    this.host = host
                                    this.type = "root"
                                    this.name = "root"
                                    this.image = "unknown"
                                }
                            } else {
                                containers.first()
                            }

                            container.id.value
                        } else {
                            null
                        }

                        // Are docker containers needed?
                        val dockerContainerIds =
                            if ((report.dockerContainers != null) && (report.dockerContainers.isNotEmpty())) {
                                val dockerContainerToContainerIdMap = mutableMapOf<String, Int>()
                                report.dockerContainers.forEach { dockerContainer ->
                                    val containers =
                                        Container.find((Containers.host eq host.id) and (Containers.type eq "docker") and (Containers.name eq dockerContainer.id))
                                    val container = if (containers.empty()) {
                                        Container.new {
                                            this.host = host
                                            this.type = "docker"
                                            this.name = dockerContainer.id
                                            this.image = dockerContainer.image
                                        }
                                    } else {
                                        containers.first()
                                    }

                                    dockerContainerToContainerIdMap[dockerContainer.id] = container.id.value
                                }
                                dockerContainerToContainerIdMap
                            } else {
                                null
                            }

                        // Now store the package information
                        if (rootContainerId != null) {
                            storePackageDetails(report.packages!!, rootContainerId, report.time)
                        }

                        if (dockerContainerIds != null) {
                            report.dockerContainers!!.forEach { dockerContainer ->
                                storePackageDetails(
                                    dockerContainer.packages!!,
                                    dockerContainerIds[dockerContainer.id]!!,
                                    report.time
                                )
                            }
                        }
                    }

                    call.respondText("OK")
                }
            }
        }

        private fun storePackageDetails(packages: List<Package>, containerId: Int, time: Long) {
            val container = Container[containerId]

            packages.forEach { pkg ->
                com.qjam.c.db.Package.new {
                    this.container = container
                    name = pkg.name
                    version = pkg.version
                    manager = pkg.manager
                    this.time = time
                }
            }
        }
    }
}




/*
 * Copyright 2020 Q-Jam B.V. 
 */
package com.qjam.c.api.v1.report.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Report(
    @SerialName("u") val uuid: String,
    @SerialName("h") val hostname: String,
    @SerialName("t") val time: Long,
    @SerialName("p") val packages: List<Package>?,
    @SerialName("d") val dockerContainers: List<DockerContainer>?,
)

@Serializable
data class Package(
    @SerialName("n") val name: String,
    @SerialName("v") val version: String,
    @SerialName("m") val manager: String,
)

@Serializable
data class DockerContainer(
    @SerialName("i") val id: String,
    @SerialName("m") val image: String,
    @SerialName("p") val packages: List<Package>?
)

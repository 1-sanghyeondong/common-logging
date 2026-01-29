package com.common.logging.common.domain

import com.fasterxml.jackson.databind.node.ObjectNode

data class StatusLogMessage(
    var desc: String? = null,
    val eventId: String,
    val traceId: String,
    val spanId: String,
    val ptxId: String,
    val requestFrom: String,
    val time: String,
    val path: String,
    val ipath: String,
    val method: String,
    val appReferer: String,
    val execTimemillis: Long,
    val responseObject: ObjectNode,
    val response: ResponseData,
    val requestBody: String?,
    val user: User,
    val deprecated: Boolean,
    val referer: String?,
    val node: String,
    val pod: String,
    val cluster: String,
    val version: String,
    val pinpointAgentId: String,
    val type: String
)

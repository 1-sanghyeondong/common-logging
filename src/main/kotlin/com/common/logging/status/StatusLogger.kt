package com.common.logging.status

import com.common.logging.common.domain.StatusLogMessage
import com.common.logging.common.utils.LoggingUtil
import com.common.logging.common.utils.filterNotEmpty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.net.URLDecoder
import kotlin.collections.set

object StatusLogger {
    private val STATUS_LOGGER = LoggerFactory.getLogger("STATUS_LOGGER")
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = ObjectMapper().registerKotlinModule()

    private fun createLogObject(
        service: String,
        phase: String,
        status: StatusLogMessage
    ): Map<String, Any> {
        val statusLog: MutableMap<String, Any> = HashMap()

        statusLog["@timestamp"] = status.time
        statusLog["service"] = service
        statusLog["execTimemillis"] = status.execTimemillis

        if (HttpStatus.valueOf(status.response.httpStatusCode).isError) {
            statusLog["responseMsg"] = status.response.msg
        }

        statusLog["statusCode"] = status.response.httpStatusCode
        statusLog["ipath"] = status.ipath
        statusLog["path"] = status.path
        statusLog["method"] = status.method
        statusLog["requestBody"] = status.requestBody ?: LoggingUtil.EMPTY_KEY
        statusLog["responseBody"] = status.response.body ?: LoggingUtil.EMPTY_KEY
        statusLog["exception"] = status.response.exception ?: LoggingUtil.EMPTY_KEY
        statusLog["phase"] = phase
        statusLog["eventId"] = status.eventId
        statusLog["traceId"] = status.traceId
        statusLog["spanId"] = status.spanId
        statusLog["ptxId"] = status.ptxId
        statusLog["userId"] = status.user.id ?: LoggingUtil.EMPTY_KEY
        statusLog["referer"] = status.referer ?: LoggingUtil.EMPTY_KEY
        statusLog["clientIp"] = status.user.clientIp
        statusLog["deviceId"] = status.user.deviceId
        statusLog["os"] = status.user.os
        statusLog["osVersion"] = status.user.osVersion
        statusLog["appVersion"] = status.user.appVersion
        statusLog["node"] = status.node
        statusLog["pod"] = status.pod
        statusLog["cluster"] = status.cluster
        statusLog["version"] = status.version
        statusLog["pinpointAgentId"] = status.pinpointAgentId
        statusLog["type"] = status.type
        statusLog["requestFrom"] = status.requestFrom
        statusLog["message"] =
            buildString {
                appendLine("req: ${status.method} ${status.path}")
                appendLine("res: ${status.response.httpStatusCode} ${status.execTimemillis}ms")
                appendLine("from: ${status.requestFrom}")
                val decodedUrlPath = urlDecode(status.path)
                if (decodedUrlPath.length != status.path.length) {
                    appendLine("decodedPath: $decodedUrlPath")
                }
            }

        return statusLog.filterNotEmpty()
    }

    fun log(
        service: String,
        phase: String,
        message: StatusLogMessage
    ) {
        try {
            val logObject: Map<String, Any> = createLogObject(service = service, phase = phase, status = message)
            STATUS_LOGGER.info("{}", objectMapper.writeValueAsString(logObject))
        } catch (e: JsonProcessingException) {
            logger.warn("Error while writing status log", e)
        }
    }

    // public for unit test
    fun urlDecode(s: String): String = URLDecoder.decode(s, "UTF-8")
}

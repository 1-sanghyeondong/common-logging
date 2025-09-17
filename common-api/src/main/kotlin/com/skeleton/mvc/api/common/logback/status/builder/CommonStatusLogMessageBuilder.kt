package com.skeleton.mvc.api.common.logback.status.builder

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.skeleton.mvc.api.common.logback.Authorization
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_ARGUMENTS
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_ARGUMENTS_ORIGINAL
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_ARGUMENTS_STRING
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_DEPRECATED
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_BODY_JSON
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_BODY_SIZE
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_CODE
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_EXCEPTION
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_MSG
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_OBJECT
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_RESULT
import com.skeleton.mvc.api.common.logback.common.domain.ResponseData
import com.skeleton.mvc.api.common.logback.common.domain.StatusAttributeKeys
import com.skeleton.mvc.api.common.logback.common.domain.StatusLogMessage
import com.skeleton.mvc.api.common.logback.common.domain.User
import com.skeleton.mvc.api.common.logback.common.utils.LogObjectMapper
import com.skeleton.mvc.api.common.logback.common.utils.LoggingUtil
import com.skeleton.mvc.api.common.logback.common.utils.LoggingUtil.KEY_MDC_PTX_ID
import com.skeleton.mvc.api.common.logback.utils.ServletRequestUtils
import com.skeleton.mvc.api.common.logback.utils.ServletRequestUtils.getLongAttribute
import org.apache.logging.log4j.util.Strings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.time.ZoneId
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class CommonStatusLogMessageBuilder(
    val responseObjectLoggingEnabled: Boolean = false,
    private val node: String,
    private val pod: String,
    private val cluster: String,
    private val version: String,
    private val pinpointAgentId: String,
    private val type: String
) : StatusLogMessageBuilder {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val emptyNode: ObjectNode = JsonNodeFactory.instance.objectNode()

    override fun buildMessage(
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse,
        responseLoggingEnabled: Boolean,
        useOriginalRequestResponseData: Boolean
    ): StatusLogMessage {
        val now = LocalDateTime.now()
        val currentTimestamp: String = StatusLogMessageBuilder.TIMESTAMP_FORMATTER.format(now)
        val path: String = servletRequest.requestURI
        val ipath: String = ServletRequestUtils.getAttribute(servletRequest, StatusAttributeKeys.IPATH, path)
        val execTimeInMillis =
            now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
                servletRequest.getAttribute(
                    StatusAttributeKeys.API_START_TIME
                ) as Long

        val params: ObjectNode =
            if (useOriginalRequestResponseData) {
                ServletRequestUtils.getAttribute(
                    servletRequest,
                    KEY_REQUEST_MAPPING_ARGUMENTS_ORIGINAL,
                    emptyNode
                )
            } else {
                ServletRequestUtils.getAttribute(
                    servletRequest,
                    KEY_REQUEST_MAPPING_ARGUMENTS,
                    emptyNode
                )
            }

        val paramsString: String? =
            ServletRequestUtils.getAttribute(
                servletRequest,
                KEY_REQUEST_MAPPING_ARGUMENTS_STRING,
                null
            )

        var responseObject: ObjectNode = emptyNode

        // FIXME responseObject 는 현재 남기지 않음
        if (responseLoggingEnabled || responseObjectLoggingEnabled) {
            responseObject =
                ServletRequestUtils.getAttribute(servletRequest, KEY_REQUEST_MAPPING_RESPONSE_OBJECT, emptyNode)
        }

        val deprecated: Boolean =
            ServletRequestUtils.getAttribute(servletRequest, KEY_REQUEST_MAPPING_DEPRECATED, false)
        val requestFrom: String =
            ServletRequestUtils.getAttribute(
                servletRequest,
                StatusAttributeKeys.REQUEST_FROM,
                LoggingUtil.EMPTY_KEY
            )
        val appReferer: String =
            ServletRequestUtils.getAttribute(
                servletRequest,
                StatusAttributeKeys.APP_REFERER,
                LoggingUtil.EMPTY_KEY
            )
        val userData = buildUserData(request = servletRequest)
        val referer = servletRequest.getHeaders(LoggingUtil.KEY_REFERER)
        return StatusLogMessage(
            eventId =
                (
                    servletRequest.getAttribute(StatusAttributeKeys.EVENT_ID)
                        ?: LoggingUtil.EMPTY_KEY
                ) as String,
            traceId = MDC.get(LoggingUtil.KEY_TRACE_ID) ?: LoggingUtil.EMPTY_KEY,
            spanId = MDC.get(LoggingUtil.KEY_SPAN_ID) ?: LoggingUtil.EMPTY_KEY,
            ptxId = MDC.get(LoggingUtil.KEY_PTX_ID) ?: MDC.get(KEY_MDC_PTX_ID) ?: LoggingUtil.EMPTY_KEY,
            requestFrom = requestFrom,
            time = currentTimestamp,
            path = path,
            ipath = ipath,
            appReferer = appReferer,
            method = servletRequest.method,
            execTimemillis = execTimeInMillis,
            responseObject = responseObject,
            response = buildResponseData(servletRequest, servletResponse, useOriginalRequestResponseData),
            user = userData,
            deprecated = deprecated,
            requestBody =
                if (params.size() != 0) {
                    params.toPrettyString()
                } else {
                    paramsString
                },
            referer =
                referer?.let {
                    if (it.hasMoreElements()) {
                        it.nextElement()
                    } else {
                        null
                    }
                },
            node = node,
            pod = pod,
            cluster = cluster,
            version = version,
            pinpointAgentId = pinpointAgentId,
            type = type
        )
    }

    private fun buildResponseData(
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse,
        useOriginalResponseData: Boolean
    ): ResponseData {
        val responseBodyJson: Any? =
            ServletRequestUtils.getAttribute(
                servletRequest,
                KEY_REQUEST_MAPPING_RESPONSE_BODY_JSON,
                null
            )

        val responseBody: Any? =
            if (useOriginalResponseData) {
                val originResult =
                    ServletRequestUtils.getAttribute(
                        servletRequest,
                        KEY_REQUEST_MAPPING_RESULT,
                        responseBodyJson
                    )

                when (originResult) {
                    is String -> LogObjectMapper.fullBodyMapper.readTree(originResult).toPrettyString()

                    else ->
                        LogObjectMapper.fullBodyMapper
                            .convertValue(
                                originResult,
                                JsonNode::class.java
                            ).toPrettyString()
                }
            } else {
                responseBodyJson
            }

        val httpStatusCode = servletResponse.status

        val responseMsg: String =
            ServletRequestUtils.getAttribute(
                servletRequest,
                KEY_REQUEST_MAPPING_RESPONSE_MSG,
                HttpStatus.valueOf(servletResponse.status).name
            )
        val responseCode: Int =
            ServletRequestUtils.getAttribute(
                servletRequest,
                KEY_REQUEST_MAPPING_RESPONSE_CODE,
                servletResponse.status
            )
        val responseSize: Int =
            ServletRequestUtils.getAttribute(
                servletRequest,
                KEY_REQUEST_MAPPING_RESPONSE_BODY_SIZE,
                0
            )

        val exception: String =
            ServletRequestUtils.getAttribute(
                servletRequest,
                KEY_REQUEST_MAPPING_RESPONSE_EXCEPTION,
                Strings.EMPTY
            )

        return ResponseData(
            code = responseCode,
            msg = responseMsg,
            httpStatusCode = httpStatusCode,
            body = responseBody,
            size = responseSize.toLong(),
            exception = exception
        )
    }

    private fun buildUserData(request: HttpServletRequest): User {
        // Set data from headers
        return User(
            id =
                request.getLongAttribute(StatusAttributeKeys.USER_ID) ?: run {
                    try {
                        request
                            .getHeader(
                                "Authorization"
                            ).replaceFirst("${Authorization.HEADER_PREFIX} ${Authorization.HEADER_USER_KEY}=".toRegex(), Strings.EMPTY)
                            .toLong()
                    } catch (ex: Exception) {
                        return@run null
                    }
                },
            clientIp = (request.getAttribute(StatusAttributeKeys.CLIENT_IP) ?: LoggingUtil.EMPTY_KEY) as String,
            deviceId = (request.getAttribute(StatusAttributeKeys.DEVICE_ID) ?: LoggingUtil.EMPTY_KEY) as String,
            os = (request.getAttribute(StatusAttributeKeys.OS) ?: LoggingUtil.EMPTY_KEY) as String,
            osVersion =
                (
                    request.getAttribute(StatusAttributeKeys.OS_VERSION)
                        ?: LoggingUtil.EMPTY_KEY
                ) as String,
            appVersion =
                (
                    request.getAttribute(StatusAttributeKeys.APP_VERSION)
                        ?: LoggingUtil.EMPTY_KEY
                ) as String
        )
    }
}

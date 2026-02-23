package com.common.logging.common

import com.common.logging.annotations.IgnorePrometheus
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.common.logging.annotations.IgnoreStatusLogging
import com.common.logging.annotations.StatusLoggerOption
import com.common.logging.common.domain.AttributeKeys
import com.common.logging.common.domain.StatusAttributeKeys
import com.common.logging.common.utils.LogObjectMapper
import com.common.logging.status.StatusLogger
import com.common.logging.status.builder.StatusLogMessageBuilder
import com.common.logging.utils.ServletRequestUtils
import com.common.logging.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_ARGUMENTS
import jakarta.servlet.ServletResponseWrapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.cors.CorsUtils
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper

const val SWAGGER_ENDPOINT = "swagger"
const val SWAGGER_V2_ENDPOINT = "/v2/api-docs"

const val ERROR_DATA_RESPONSE_KEY = "errorData"

@SuppressWarnings("deprecated")
class StatusLoggingHandlerInterceptor(
    private val service: String,
    private val phase: String,
    private val messageBuilder: StatusLogMessageBuilder
) : HandlerInterceptor {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val emptyNode: ObjectNode = JsonNodeFactory.instance.objectNode()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        request.setAttribute(StatusAttributeKeys.API_START_TIME, System.currentTimeMillis())
        request.setAttribute(
            StatusAttributeKeys.IPATH,
            request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
        )
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return
        }

        val logSent = request.getAttribute(StatusAttributeKeys.LOG_SENT) as Boolean?
        if (logSent == true) {
            return
        }
        request.setAttribute(StatusAttributeKeys.LOG_SENT, true)
        if (isIgnoreRequest(handler)) {
            return
        }

        if (request.requestURI.contains(SWAGGER_ENDPOINT) || request.requestURI.contains(SWAGGER_V2_ENDPOINT)) {
            return
        }

        setFailoverRequestResponseLog(request, response)

        val statusLoggerOption: StatusLoggerOption? = getStatusLoggerOption(handler)
        val statusLogMessage =
            messageBuilder.buildMessage(
                servletRequest = request,
                servletResponse = response,
                responseLoggingEnabled = true,
                useOriginalRequestResponseData = statusLoggerOption != null && statusLoggerOption.fullBody
            )

        StatusLogger.log(service, phase, statusLogMessage)
    }

    private fun getStatusLoggerOption(handler: Any): StatusLoggerOption? {
        if (handler !is HandlerMethod) {
            return null
        }
        return handler.getMethodAnnotation(StatusLoggerOption::class.java)
    }

    private fun isIgnoreRequest(handler: Any): Boolean {
        if (handler !is HandlerMethod) {
            return false
        }
        return (
            handler.getMethodAnnotation(IgnoreStatusLogging::class.java) != null ||
                handler.getMethodAnnotation(IgnorePrometheus::class.java) != null
        )
    }

    // aspect-around 에서 로깅 실패시 servlet request, response 에서 꺼내서 로깅
    private fun setFailoverRequestResponseLog(
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        setFailoverRequestLog(request)
        setFailoverResponseLog(request, response)
    }

    private fun setFailoverRequestLog(request: HttpServletRequest) {
        if (request is ContentCachingRequestWrapper &&
            ServletRequestUtils.getAttribute(request, KEY_REQUEST_MAPPING_ARGUMENTS, emptyNode).size() == 0
        ) {
            val failoverRequest = generateFailoverRequest(request)
            request.setAttribute(AttributeKeys.KEY_REQUEST_MAPPING_ARGUMENTS_STRING, failoverRequest)
        }
    }

    private fun generateFailoverRequest(request: ContentCachingRequestWrapper): String? {
        val requestString = String(request.contentAsByteArray)
        return if (requestString.trim().isNotEmpty()) {
            if (request.contentType == APPLICATION_JSON_VALUE) {
                try {
                    LogObjectMapper.fullBodyMapper
                        .readTree(requestString)
                        .toPrettyString()
                } catch (ex: Exception) {
                    logger.error("request logging parse error | request: {}, error_message: {}", requestString, ex.message, ex)
                    throw ex
                }
            } else {
                requestString
            }
        } else {
            null
        }
    }

    private fun setFailoverResponseLog(
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        if (HttpStatus.valueOf(response.status).is2xxSuccessful.not() ||
            request.getAttribute(AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_EXCEPTION) != null
        ) {
            val contentCachingResponseWrapper = getContentCachingResponseWrapper(response)
            contentCachingResponseWrapper?.also {
                val errorResponse = generateErrorResponse(request = request, response = it)
                request.setAttribute(AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_BODY_JSON, errorResponse)
                request.setAttribute(AttributeKeys.KEY_REQUEST_MAPPING_RESULT, errorResponse)
            }
        }
    }

    private fun getContentCachingResponseWrapper(response: HttpServletResponse): ContentCachingResponseWrapper? =
        try {
            if (response is ContentCachingResponseWrapper) {
                response
            } else if ((response as? ServletResponseWrapper)?.response is ContentCachingResponseWrapper) {
                (response as ServletResponseWrapper).response as ContentCachingResponseWrapper
            } else {
                null
            }
        } catch (ex: Exception) {
            logger.warn("status logging 'UNKNOWN RESPONSE' type | error_message: {}", ex.message, ex)
            null
        }

    private fun generateErrorResponse(
        request: HttpServletRequest,
        response: ContentCachingResponseWrapper
    ): String? {
        val responseString = String(response.contentAsByteArray)
        return if (responseString.trim().isNotEmpty()) {
            if (request.contentType == APPLICATION_JSON_VALUE) {
                try {
                    val root: JsonNode = LogObjectMapper.mapper.readTree(responseString)
                    errorDataMasking(root)
                    return root.toPrettyString()
                } catch (ex: Exception) {
                    logger.error("response logging parse error | request: {}, error_message: {}", responseString, ex.message, ex)
                    throw ex
                }
            } else {
                responseString
            }
        } else {
            null
        }
    }

    private fun errorDataMasking(root: JsonNode) {
        if (root is ObjectNode) {
            root.get(ERROR_DATA_RESPONSE_KEY).let { errorDataNode ->
                if (errorDataNode is ObjectNode) {
                    errorDataNode.fieldNames()
                }
            }
        }
    }
}

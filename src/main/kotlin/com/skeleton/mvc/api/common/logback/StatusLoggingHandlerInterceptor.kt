package com.skeleton.mvc.api.common.logback

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.skeleton.mvc.api.common.logback.annotations.IgnoreProm
import com.skeleton.mvc.api.common.logback.annotations.IgnoreStatusLogging
import com.skeleton.mvc.api.common.logback.annotations.StatusLoggerOption
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_ARGUMENTS
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_ARGUMENTS_STRING
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_BODY_JSON
import com.skeleton.mvc.api.common.logback.common.domain.AttributeKeys.KEY_REQUEST_MAPPING_RESULT
import com.skeleton.mvc.api.common.logback.common.domain.StatusAttributeKeys
import com.skeleton.mvc.api.common.logback.common.utils.LogObjectMapper
import com.skeleton.mvc.api.common.logback.status.StatusLogger
import com.skeleton.mvc.api.common.logback.status.builder.StatusLogMessageBuilder
import com.skeleton.mvc.api.common.logback.utils.ServletRequestUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.cors.CorsUtils
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import javax.servlet.ServletResponseWrapper
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@SuppressWarnings("deprecated")
class StatusLoggingHandlerInterceptor(
    private val service: String,
    private val phase: String,
    private val messageBuilder: StatusLogMessageBuilder
) : HandlerInterceptorAdapter() {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val emptyNode: ObjectNode = JsonNodeFactory.instance.objectNode()

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
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

        if (request.requestURI.contains("swagger") ||
            request.requestURI.contains("/v2/api-docs")
        ) {
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
                handler.getMethodAnnotation(IgnoreProm::class.java) != null
        )
    }

    // Aspect-Around 에서 로깅 실패시, Servlet Request, Response 에서 꺼내서 로깅
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
            request.setAttribute(KEY_REQUEST_MAPPING_ARGUMENTS_STRING, failoverRequest)
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
                } catch (e: Exception) {
                    logger.error("RequestLogging Parse Error : $requestString", e)
                    throw e
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
                request.setAttribute(KEY_REQUEST_MAPPING_RESPONSE_BODY_JSON, errorResponse)
                request.setAttribute(KEY_REQUEST_MAPPING_RESULT, errorResponse)
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
        } catch (e: Exception) {
            logger.warn("Status Logging - UNKNOWN RESPONSE type", e)
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
                } catch (e: Exception) {
                    logger.error("ResponseLogging Parse Error : $responseString", e)
                    throw e
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
            root.get("errorData").let { errorDataNode ->
                if (errorDataNode is ObjectNode) {
                    errorDataNode.fieldNames()
                }
            }
        }
    }
}

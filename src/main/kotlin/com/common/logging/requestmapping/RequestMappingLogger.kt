package com.common.logging.requestmapping

import com.common.logging.common.domain.AttributeKeys
import com.common.logging.common.utils.LogObjectMapper
import com.common.logging.requestmapping.dto.MethodArgument
import com.common.logging.requestmapping.dto.RequestLog
import com.common.logging.utils.ClientIP
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.apache.commons.lang3.StringUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import java.lang.Deprecated
import java.lang.reflect.Method

class RequestMappingLogger(
    val responseMaxLength: Int = 0,
    val excludedArgsClasses: MutableSet<Class<*>>
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        excludedArgsClasses.addAll(
            hashSetOf(
                ServletRequest::class.java,
                ServletResponse::class.java,
                HttpSession::class.java
            )
        )
    }

    @Throws(Throwable::class)
    fun aroundRequestMapping(
        request: HttpServletRequest,
        joinPoint: ProceedingJoinPoint
    ): Any? {
        val path = request.requestURI
        val clientIp: String = ClientIP.of(request)
        val signature: String = joinPoint.signature.toShortString()
        logRequest(request, path, clientIp, signature, joinPoint)
        var result: Any? = null
        var responseObject: Any? = null
        var responseMsg: String? = AttributeKeys.DEFAULT_SUCCESS_MSG
        val responseCode = AttributeKeys.DEFAULT_SUCCESS_RESPONSE_CODE
        var exception: String? = null
        try {
            result = joinPoint.proceed()
            responseObject = result
        } catch (e: Throwable) {
            val exceptionMessage: String = e.message ?: ""
            responseMsg = exceptionMessage
            exception = e.stackTraceToString()
            throw e
        } finally {
            logResponse(
                request,
                result,
                responseObject,
                responseCode,
                responseMsg,
                exception,
                joinPoint
            )
        }
        return result
    }

    private fun logRequest(
        request: HttpServletRequest,
        path: String,
        clientIp: String,
        signature: String,
        joinPoint: ProceedingJoinPoint
    ) {
        try {
            val methodArguments: List<MethodArgument> = extractArguments(joinPoint)

            request.setAttribute(
                AttributeKeys.KEY_REQUEST_MAPPING_ARGUMENTS_ORIGINAL,
                RequestLog.Companion
                    .of(
                        mapper = LogObjectMapper.fullBodyMapper,
                        path = path,
                        clientIp = clientIp,
                        signature = signature,
                        methodArguments = methodArguments,
                        excludedArgsClasses = excludedArgsClasses
                    ).arguments
            )

            val requestLog =
                RequestLog.Companion.of(
                    mapper = LogObjectMapper.mapper,
                    path = path,
                    clientIp = clientIp,
                    signature = signature,
                    methodArguments = methodArguments,
                    excludedArgsClasses = excludedArgsClasses
                )

            setArgumentsToRequestAttribute(request, requestLog)
        } catch (e: Exception) {
            logger.warn("Exception on logRequest", e)
        }
    }

    private fun extractArguments(joinPoint: ProceedingJoinPoint): List<MethodArgument> {
        val methodSignature: MethodSignature = joinPoint.signature as MethodSignature
        val names: Array<String> = methodSignature.parameterNames

        val annotations = methodSignature.method.parameterAnnotations
        val args: Array<Any?> = joinPoint.args

        val nameLength = names.size
        val valueLength = args.size
        require(nameLength == valueLength) { "names and values length must be same~!" }
        val methodArguments = arrayOfNulls<MethodArgument>(names.size)

        for (i in methodArguments.indices) {
            val name = names[i]
            val value = args[i]
            val annotationArray = annotations[i]

            methodArguments[i] = MethodArgument.Companion.of(name, value, annotationArray)
        }
        return methodArguments.filterNotNull()
    }

    private fun <A : Annotation?> hasMethodAnnotation(
        joinPoint: ProceedingJoinPoint,
        annotationClass: Class<A>
    ): Boolean {
        val methodSignature: MethodSignature = joinPoint.signature as MethodSignature
        val annotation: A = methodSignature.method.getAnnotation(annotationClass)
        return annotation != null
    }

    private fun setArgumentsToRequestAttribute(
        request: HttpServletRequest,
        requestLog: RequestLog
    ) {
        request.setAttribute(AttributeKeys.KEY_REQUEST_MAPPING_ARGUMENTS, requestLog.arguments)
    }

    private fun logResponse(
        request: HttpServletRequest,
        result: Any?,
        responseObject: Any?,
        responseCode: Int,
        responseMsg: String?,
        exception: String?,
        joinPoint: ProceedingJoinPoint
    ) {
        try {
            request.setAttribute(AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_OBJECT, convertToObjectNode(responseObject))
            val responseBodyJson = toJsonOrSecret(result, joinPoint)
            request.setAttribute(AttributeKeys.KEY_REQUEST_MAPPING_RESULT, result)
            val abbreviateBodyJson = abbreviate(responseBodyJson, responseMaxLength)
            request.setAttribute(AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_BODY_JSON, abbreviateBodyJson)
            request.setAttribute(AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_BODY_SIZE, responseBodyJson.length)
            request.setAttribute(AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_CODE, responseCode)
            request.setAttribute(AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_MSG, responseMsg)
            request.setAttribute(AttributeKeys.KEY_REQUEST_MAPPING_RESPONSE_EXCEPTION, exception)
            request.setAttribute(AttributeKeys.KEY_REQUEST_MAPPING_DEPRECATED, isDeprecated(joinPoint))
        } catch (e: Exception) {
            logger.warn("Exception on logResponse", e)
        }
    }

    private fun toJsonOrSecret(
        result: Any?,
        joinPoint: ProceedingJoinPoint
    ): String = toJsonString(result ?: StringUtils.EMPTY)

    private fun convertToObjectNode(responseObject: Any?): ObjectNode {
        var jsonNode =
            if (responseObject != null) {
                LogObjectMapper.mapper.convertValue(
                    responseObject,
                    JsonNode::class.java
                )
            } else {
                NullNode.getInstance()
            }

        if (jsonNode == null) {
            jsonNode = NullNode.getInstance()
        }
        return if (jsonNode.isObject) {
            jsonNode as ObjectNode
        } else {
            val objectNode = JsonNodeFactory.instance.objectNode()
            objectNode.replace("response", jsonNode)
            objectNode
        }
    }

    private fun isDeprecated(joinPoint: ProceedingJoinPoint): Boolean {
        val sign: Signature = joinPoint.signature
        return if (sign is MethodSignature) {
            isDeprecated(sign)
        } else {
            false
        }
    }

    private fun isDeprecated(sign: MethodSignature): Boolean = isDeprecated(sign.declaringType) || isDeprecated(sign.method)

    private fun isDeprecated(method: Method): Boolean = method.getDeclaredAnnotation(Deprecated::class.java) != null

    private fun isDeprecated(type: Class<*>): Boolean = type.getDeclaredAnnotation(Deprecated::class.java) != null

    private fun toJsonString(baseLog: Any): String =
        try {
            LogObjectMapper.mapper.writeValueAsString(baseLog)
        } catch (e: JsonProcessingException) {
            logger.warn("BaseLog.toJsonString fail.", e)
            ""
        }

    private fun abbreviate(
        jsonString: String,
        maxLength: Int
    ): String =
        try {
            if (maxLength > 0 && jsonString.length > maxLength) {
                jsonString.substring(0, maxLength - 1)
            } else {
                jsonString
            }
        } catch (e: Exception) {
            logger.warn("BaseLog.toJsonString abbreviate fail.", e)
            ""
        }
}

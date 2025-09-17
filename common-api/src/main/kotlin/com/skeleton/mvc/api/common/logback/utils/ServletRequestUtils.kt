package com.skeleton.mvc.api.common.logback.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequest

object ServletRequestUtils {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun <T> getAttribute(
        request: ServletRequest,
        key: String,
        defaultValue: T
    ): T =
        try {
            val value: T? = request.getAttribute(key) as T
            value ?: defaultValue
        } catch (e: Exception) {
            log.warn("Error on get an attribute with key '{}'", key, e)
            defaultValue
        }

    fun HttpServletRequest.getLongAttribute(name: String): Long? =
        try {
            (getAttribute(name) as String?)?.toLong()
        } catch (e: Exception) {
            null
        }
}

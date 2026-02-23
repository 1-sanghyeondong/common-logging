package com.common.logging.utils

import jakarta.servlet.ServletRequest
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ServletRequestUtils {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun <T> getAttribute(request: ServletRequest, key: String, defaultValue: T): T =
        try {
            val value: T? = request.getAttribute(key) as T
            value ?: defaultValue
        } catch (ex: Exception) {
            log.warn("error on get an attribute with key: {}, message: {}", key, ex.message, ex)
            defaultValue
        }

    fun HttpServletRequest.getLongAttribute(name: String): Long? =
        try {
            (getAttribute(name) as String?)?.toLong()
        } catch (ex: Exception) {
            null
        }
}

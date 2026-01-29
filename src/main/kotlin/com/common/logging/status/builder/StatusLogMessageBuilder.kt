package com.common.logging.status.builder

import com.common.logging.common.domain.StatusLogMessage
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.format.DateTimeFormatter

interface StatusLogMessageBuilder {
    companion object {
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS")
        val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'+09:00'")
    }

    fun buildMessage(servletRequest: HttpServletRequest, servletResponse: HttpServletResponse, responseLoggingEnabled: Boolean, useOriginalRequestResponseData: Boolean): StatusLogMessage
}

package com.skeleton.mvc.api.common.logback.extension

import com.skeleton.mvc.api.common.logback.common.utils.LoggingUtil
import org.slf4j.MDC
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal fun HttpServletRequest.getEventId(): String {
    var eventId: String? = this.getHeader(LoggingUtil.KEY_EVENT_ID)

    if (eventId.isNullOrEmpty()) {
        eventId = MDC.get(LoggingUtil.KEY_EVENT_ID)
    }

    if (eventId.isNullOrEmpty()) {
        eventId = MDC.get(LoggingUtil.KEY_TRACE_ID)
    }

    if (eventId.isNullOrEmpty()) {
        eventId = LoggingUtil.EMPTY_KEY
    }

    return eventId
}

internal fun HttpServletRequest.getIpath(): String? = getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String?

internal fun HttpServletResponse.putHeaderIfAbsent(
    name: String,
    value: String
) {
    if (this.getHeaders(name).isEmpty()) {
        this.addHeader(name, value)
    }
}

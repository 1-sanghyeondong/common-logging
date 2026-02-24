package com.common.logging.common

import com.common.logging.common.domain.StatusAttributeKeys
import com.common.logging.common.utils.LoggingUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

class MdcTraceFilter : OncePerRequestFilter() {

    companion object {
        const val HEADER_TRACE_PARENT = "traceparent"
        const val HEADER_B3 = "b3"
        const val HEADER_B3_TRACE_ID = "X-B3-TraceId"
        const val HEADER_B3_SPAN_ID  = "X-B3-SpanId"

        const val HEADER_USER_ID      = "X-User-Id"
        const val HEADER_DEVICE_ID    = "X-Device-Id"
        const val HEADER_REQUEST_FROM = "X-Request-From"
        const val HEADER_REQUEST_ID   = "X-Request-Id"

        const val MDC_KEY_REQUEST_ID = "requestId"

        val MDC_KEYS_MANAGED = setOf(
            LoggingUtil.KEY_USER_ID,
            LoggingUtil.KEY_DEVICE_ID,
            LoggingUtil.KEY_REQUEST_FROM,
            MDC_KEY_REQUEST_ID,
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val injectedKeys = mutableSetOf<String>()
        try {
            injectTraceContext(request, injectedKeys)
            injectCustomContext(request, injectedKeys)
            filterChain.doFilter(request, response)
        } finally {
            injectedKeys.forEach { MDC.remove(it) }
        }
    }

    private fun injectTraceContext(request: HttpServletRequest, injected: MutableSet<String>) {
        val traceparent = request.getHeader(HEADER_TRACE_PARENT)
        if (traceparent != null) {
            parseTraceparent(traceparent)?.let { (traceId, spanId) ->
                setMdc(LoggingUtil.KEY_TRACE_ID, traceId, injected)
                setMdc(LoggingUtil.KEY_SPAN_ID,  spanId,  injected)
            }
            return
        }

        val b3Single = request.getHeader(HEADER_B3)
        if (b3Single != null) {
            parseB3Single(b3Single)?.let { (traceId, spanId) ->
                setMdc(LoggingUtil.KEY_TRACE_ID, traceId, injected)
                setMdc(LoggingUtil.KEY_SPAN_ID,  spanId,  injected)
            }
            return
        }

        val b3TraceId = request.getHeader(HEADER_B3_TRACE_ID)
        val b3SpanId  = request.getHeader(HEADER_B3_SPAN_ID)
        if (b3TraceId != null) {
            setMdc(LoggingUtil.KEY_TRACE_ID, b3TraceId, injected)
            if (b3SpanId != null) setMdc(LoggingUtil.KEY_SPAN_ID, b3SpanId, injected)
            return
        }

        // 트레이스 헤더 없음 — Micrometer 가 MDC 에 이미 주입한 값이 있으면 그대로 사용.
        // 없으면 UUID 로 자체 생성 (Micrometer HTTP 인스트루멘테이션 미설정 환경 Fallback)
        if (MDC.get(LoggingUtil.KEY_TRACE_ID).isNullOrEmpty()) {
            val traceId = UUID.randomUUID().toString().replace("-", "")
            val spanId  = UUID.randomUUID().toString().replace("-", "").take(16)
            setMdc(LoggingUtil.KEY_TRACE_ID, traceId, injected)
            setMdc(LoggingUtil.KEY_SPAN_ID,  spanId,  injected)
        }
    }

    private fun parseTraceparent(value: String): Pair<String, String>? {
        val parts = value.split("-")
        return if (parts.size >= 4) parts[1] to parts[2] else null
    }

    private fun parseB3Single(value: String): Pair<String, String>? {
        val parts = value.split("-")
        return if (parts.size >= 2) parts[0] to parts[1] else null
    }

    private fun injectCustomContext(request: HttpServletRequest, injected: MutableSet<String>) {
        request.getHeader(HEADER_USER_ID)?.let { userId ->
            setMdc(LoggingUtil.KEY_USER_ID, userId, injected)
            userId.toLongOrNull()?.let { request.setAttribute(StatusAttributeKeys.USER_ID, it) }
        }
        request.getHeader(HEADER_DEVICE_ID)?.let { deviceId ->
            setMdc(LoggingUtil.KEY_DEVICE_ID, deviceId, injected)
            request.setAttribute(StatusAttributeKeys.DEVICE_ID, deviceId)
        }
        request.getHeader(HEADER_REQUEST_FROM)?.let { requestFrom ->
            setMdc(LoggingUtil.KEY_REQUEST_FROM, requestFrom, injected)
            request.setAttribute(StatusAttributeKeys.REQUEST_FROM, requestFrom)
        }

        // requestId: 헤더에 없으면 UUID 생성
        val requestId = request.getHeader(HEADER_REQUEST_ID) ?: UUID.randomUUID().toString()
        setMdc(MDC_KEY_REQUEST_ID, requestId, injected)
        request.setAttribute(MDC_KEY_REQUEST_ID, requestId)
    }

    private fun setMdc(key: String, value: String, injected: MutableSet<String>) {
        MDC.put(key, value)
        injected.add(key)
    }
}

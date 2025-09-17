package com.skeleton.mvc.api.common.logback.common.utils

import org.apache.logging.log4j.util.Strings
import org.springframework.http.HttpHeaders
import org.springframework.web.server.ServerWebExchange

object ClientIP {
    fun of(exchange: ServerWebExchange): String {
        val headers: HttpHeaders = exchange.request.headers
        var clientIp = headers.getFirst("X-Forwarded-For")
        if (isInvalidIp(clientIp)) {
            clientIp = headers.getFirst("X-Real-IP")
        }
        if (isInvalidIp(clientIp)) {
            clientIp = exchange.request.remoteAddress?.hostString
        }
        return clientIp
            ?.split(",")
            ?.toTypedArray()
            ?.get(0)
            ?.trim { it <= ' ' } ?: Strings.EMPTY
    }

    private fun isInvalidIp(clientIp: String?): Boolean = clientIp.isNullOrEmpty()
}

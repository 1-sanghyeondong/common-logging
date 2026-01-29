package com.skeleton.mvc.api.common.logback.utils

import javax.servlet.http.HttpServletRequest

object ClientIP {
    fun of(request: HttpServletRequest): String {
        var clientIp = request.getHeader("X-Forwarded-For")
        if (isInvalidIp(clientIp)) {
            clientIp = request.getHeader("X-Real-IP")
        }
        if (isInvalidIp(clientIp)) {
            clientIp = request.remoteAddr
        }
        return clientIp.split(",").toTypedArray()[0].trim { it <= ' ' }
    }

    private fun isInvalidIp(clientIp: String?): Boolean = clientIp.isNullOrEmpty()
}

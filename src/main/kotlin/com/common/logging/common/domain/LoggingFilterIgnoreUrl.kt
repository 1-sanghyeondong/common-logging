package com.common.logging.common.domain

object LoggingFilterIgnoreUrl {
    private val urlList = setOf("/actuator/prometheus")

    fun contains(url: String) = urlList.contains(url)
}

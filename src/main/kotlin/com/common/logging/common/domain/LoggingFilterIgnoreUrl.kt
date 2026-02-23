package com.common.logging.common.domain

const val STATUS_LOGGING_FILTER_IGNORE_URL = "/actuator/prometheus"

object LoggingFilterIgnoreUrl {
    private val urlList = setOf(STATUS_LOGGING_FILTER_IGNORE_URL)

    fun contains(url: String) = urlList.contains(url)
}

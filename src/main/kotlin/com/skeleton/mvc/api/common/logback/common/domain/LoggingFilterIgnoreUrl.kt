package com.skeleton.mvc.api.common.logback.common.domain

object LoggingFilterIgnoreUrl {
    private val urlList = setOf("/actuator/prometheus")

    fun contains(url: String) = urlList.contains(url)
}

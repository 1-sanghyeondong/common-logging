package com.skeleton.mvc.api.common.logback.common.domain

object StatusAttributeKeys {
    private const val PREFIX = "STATUS_ATTRIBUTE_KEY_"
    const val API_START_TIME = PREFIX + "API_START_TIME"
    const val IPATH = PREFIX + "IPATH"
    const val LOG_SENT = PREFIX + "LOG_SENT"
    const val EVENT_ID = PREFIX + "EVENT_ID"
    const val USER_ID = PREFIX + "USER_ID"
    const val REQUEST_FROM = PREFIX + "REQUEST_FROM"
    const val APP_REFERER = PREFIX + "APP_REFERER"
    const val DEVICE_ID = PREFIX + "DEVICE_ID"
    const val CLIENT_IP = PREFIX + "CLIENT_IP"

    const val OS = PREFIX + "OS"
    const val OS_VERSION = PREFIX + "OS_VERSION"
    const val APP_VERSION = PREFIX + "APP_VERSION"
}

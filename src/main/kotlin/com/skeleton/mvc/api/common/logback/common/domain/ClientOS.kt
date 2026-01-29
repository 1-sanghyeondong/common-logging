package com.skeleton.mvc.api.common.logback.common.domain

import org.apache.logging.log4j.util.Strings

data class ClientOS(
    val osType: OSType = OSType.unknown,
    val osVersion: String = Strings.EMPTY
)

enum class OSType {
    ios,
    android,
    unknown
}

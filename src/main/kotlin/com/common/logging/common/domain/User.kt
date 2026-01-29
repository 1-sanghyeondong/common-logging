package com.common.logging.common.domain

data class User(
    val id: Long?,
    val deviceId: String,
    val clientIp: String,
    val os: String,
    val osVersion: String,
    val appVersion: String
)

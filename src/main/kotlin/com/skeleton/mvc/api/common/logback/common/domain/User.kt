package com.skeleton.mvc.api.common.logback.common.domain

data class User(
    val id: Long?,
    val deviceId: String,
    val clientIp: String,
    val os: String,
    val osVersion: String,
    val appVersion: String
)

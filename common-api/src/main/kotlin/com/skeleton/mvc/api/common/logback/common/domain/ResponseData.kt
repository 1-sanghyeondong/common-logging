package com.skeleton.mvc.api.common.logback.common.domain

data class ResponseData(
    val code: Int,
    val msg: String,
    val httpStatusCode: Int,
    val body: Any?,
    val size: Long? = null,
    val exception: String?
)

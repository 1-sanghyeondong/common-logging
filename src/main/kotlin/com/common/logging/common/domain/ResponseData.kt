package com.common.logging.common.domain

data class ResponseData(
    val code: Int,
    val msg: String,
    val httpStatusCode: Int,
    val body: Any?,
    val size: Long? = null,
    val exception: String?
)

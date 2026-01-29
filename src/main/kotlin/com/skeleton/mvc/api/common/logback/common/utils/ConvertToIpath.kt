package com.skeleton.mvc.api.common.logback.common.utils

import java.net.URLDecoder

object ConvertToIpath {
    private const val PathParam = "{pathParam}"
    private val lengthOver50 = Regex("""[A-Za-z\d]{50,}""")
    private val uuidFormat = Regex("""^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}${'$'}""")
    private val dateFormat = Regex("""[0-9\-]+""")
    private val valueFormat = Regex("""[0-9A-Z_]+""") // 대문자와 숫자 언더스코어로만 이루어진 형태

    operator fun invoke(path: String): String {
        val pathWithoutQueryParams = path.substringBefore(delimiter = "?")
        return pathWithoutQueryParams.split("/").joinToString(separator = "/") {
            when {
                it.isUrlEncoded() -> PathParam
                it matches lengthOver50 -> PathParam
                it matches uuidFormat -> PathParam
                it matches dateFormat -> PathParam
                it matches valueFormat -> PathParam
                else -> it
            }
        }
    }

    private fun String.isUrlEncoded(): Boolean = this != URLDecoder.decode(this, "UTF-8")
}

package com.common.logging.common

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.pattern.MessageConverter
import ch.qos.logback.classic.pattern.RootCauseFirstThrowableProxyConverter
import ch.qos.logback.classic.pattern.ThrowableProxyConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import org.apache.commons.lang3.StringEscapeUtils
import org.apache.commons.lang3.StringUtils

class EscapedPatternLayout : PatternLayout() {
    private var defaultConverterMap: MutableMap<String, String> = HashMap()

    override fun start() {
        this.defaultConverterMap = HashMap(super.getDefaultConverterMap())
        this.defaultConverterMap["m"] = JsonSafeMessageConverter::class.java.name
        this.defaultConverterMap["msg"] = JsonSafeMessageConverter::class.java.name
        this.defaultConverterMap["message"] = JsonSafeMessageConverter::class.java.name
        this.defaultConverterMap["metric"] = customJsonSafeMessageConveter::class.java.name
        this.defaultConverterMap["ex"] = JsonSafeThrowableProxyConverter::class.java.name
        this.defaultConverterMap["exception"] = JsonSafeThrowableProxyConverter::class.java.name
        this.defaultConverterMap["rEx"] = JsonSafeRootCauseFirstThrowableProxyConverter::class.java.name
        this.defaultConverterMap["rootException"] = JsonSafeRootCauseFirstThrowableProxyConverter::class.java.name
        super.start()
    }

    override fun getDefaultConverterMap(): Map<String, String> = defaultConverterMap

    override fun doLayout(event: ILoggingEvent): String = super.doLayout(event)

    class JsonSafeMessageConverter : MessageConverter() {
        override fun convert(event: ILoggingEvent): String = jsonSafeReplace(super.convert(event))
    }

    class customJsonSafeMessageConveter : MessageConverter() {
        override fun convert(event: ILoggingEvent): String = customJsonSafeReplace(super.convert(event))
    }

    class JsonSafeThrowableProxyConverter : ThrowableProxyConverter() {
        override fun convert(event: ILoggingEvent): String = jsonSafeReplace(super.convert(event))
    }

    class JsonSafeRootCauseFirstThrowableProxyConverter : RootCauseFirstThrowableProxyConverter() {
        override fun convert(event: ILoggingEvent): String = jsonSafeReplace(super.convert(event))
    }

    companion object {
        fun jsonSafeReplace(origin: String?): String = StringEscapeUtils.escapeJson(origin)

        /**
         * sonSafeReplace 에서 "/" 문자를 치환하는 부분을 제거하기 위해서 정의함.
         */
        fun customJsonSafeReplace(origin: String?): String {
            var message = StringUtils.replace(origin, "\t", "\\t")
            message = StringUtils.replace(message, "\n", "\\n")
            message = StringUtils.replace(message, "\r", "\\r")
            return StringUtils.replace(message, "\"", "\\\"")
        }
    }
}
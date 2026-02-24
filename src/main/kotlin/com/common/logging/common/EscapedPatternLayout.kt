package com.common.logging.common

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.pattern.MessageConverter
import ch.qos.logback.classic.pattern.RootCauseFirstThrowableProxyConverter
import ch.qos.logback.classic.pattern.ThrowableProxyConverter
import ch.qos.logback.classic.spi.ILoggingEvent

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
        fun jsonSafeReplace(origin: String?): String {
            if (origin.isNullOrEmpty()) return ""
            return buildString(origin.length + 16) {
                for (ch in origin) {
                    when (ch) {
                        '"'  -> append("\\\"")
                        '\\' -> append("\\\\")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        '\b' -> append("\\b")
                        '\u000C' -> append("\\f")
                        else -> if (ch < ' ') append("\\u%04x".format(ch.code)) else append(ch)
                    }
                }
            }
        }

        fun customJsonSafeReplace(origin: String?): String =
            (origin ?: "")
                .replace("\t", "\\t")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\"", "\\\"")
    }
}
package com.common.logging.testutil

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 테스트 전용 인메모리 Logback Appender
 */
class InMemoryLogAppender : AppenderBase<ILoggingEvent>() {

    private val events: MutableList<ILoggingEvent> = CopyOnWriteArrayList()
    private val mapper = ObjectMapper().registerKotlinModule()

    override fun append(event: ILoggingEvent) {
        events.add(event)
    }

    /** 캡처된 모든 로그 이벤트 */
    fun allEvents(): List<ILoggingEvent> = events.toList()

    /** 마지막 이벤트의 raw 메시지 문자열 */
    fun lastMessage(): String = events.last().formattedMessage

    /** 마지막 이벤트의 메시지를 JSON Map으로 파싱 */
    fun lastJsonMap(): Map<String, Any?> = mapper.readValue(lastMessage())

    /** 캡처된 모든 메시지를 JSON Map 리스트로 */
    fun allJsonMaps(): List<Map<String, Any?>> =
        events.map { mapper.readValue(it.formattedMessage) }

    /** 이벤트 목록 초기화 */
    fun clear() = events.clear()

    /** 이벤트가 하나 이상 캡처되었는지 여부 */
    fun hasEvents(): Boolean = events.isNotEmpty()

    /** Logback Logger에서 이 Appender를 제거 */
    fun detach() {
        stop()
        val loggerContext = org.slf4j.LoggerFactory.getILoggerFactory()
            as ch.qos.logback.classic.LoggerContext
        loggerContext.getLogger(attachedLoggerName ?: return).detachAppender(this)
    }

    private var attachedLoggerName: String? = null

    companion object {
        /**
         * 지정한 이름의 Logger에 InMemoryLogAppender를 붙이고 반환한다.
         *
         * @param loggerName Logback logger 이름 (e.g. "STATUS_LOGGER")
         */
        fun attachTo(loggerName: String): InMemoryLogAppender {
            val loggerContext = org.slf4j.LoggerFactory.getILoggerFactory()
                as ch.qos.logback.classic.LoggerContext
            val logger = loggerContext.getLogger(loggerName)

            val appender = InMemoryLogAppender()
            appender.attachedLoggerName = loggerName
            appender.context = loggerContext
            appender.name = "InMemoryLogAppender-$loggerName"
            appender.start()

            logger.addAppender(appender)
            return appender
        }
    }
}

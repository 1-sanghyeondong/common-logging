package com.common.logging.common

import com.common.logging.annotations.EnableLogging
import com.common.logging.common.utils.LoggingUtil
import com.common.logging.status.builder.CommonStatusLogMessageBuilder
import com.common.logging.status.builder.StatusLogMessageBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.util.pattern.PathPatternParser

@ConditionalOnBean(annotation = [EnableLogging::class])
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class StatusLoggingConfiguration : WebMvcConfigurer {

    @Value("\${app.id}")
    private lateinit var service: String

    @Value("\${spring.profiles.active:#{null}}")
    private lateinit var phase: String

    @Value("\${status-logger.response-logging.enabled:false}")
    private val responseObjectLoggingEnabled: Boolean = false

    @Value("\${status-logger.content-caching.enabled:true}")
    private val enableContentCaching: Boolean = true

    @Value("\${status-logger.content-caching.ignore-path-patterns:}")
    private val contentCachingIgnorePathPatterns: List<String> = listOf()

    @Bean
    fun statusLoggingHandlerInterceptor(messageBuilder: StatusLogMessageBuilder): StatusLoggingHandlerInterceptor {
        return StatusLoggingHandlerInterceptor(service = service, phase = phase, messageBuilder = messageBuilder)
    }

    @Bean
    @ConditionalOnMissingBean(StatusLogMessageBuilder::class)
    fun defaultStatusLogMessageBuilder(
        @Value("#{systemEnvironment['NODE_NAME'] ?: '-'}") node: String = LoggingUtil.EMPTY_KEY,
        @Value("#{systemEnvironment['HOSTNAME'] ?: '-'}") pod: String,
        @Value("#{systemEnvironment['CLUSTER'] ?: '-'}") cluster: String,
        @Value("#{systemEnvironment['VERSION'] ?: '-'}") version: String,
        @Value("#{systemEnvironment['PINPOINT_ID'] ?: '-'}") pinpointAgentId: String,
        @Value("#{systemEnvironment['APP_TYPE'] ?: '-'}") type: String
    ): StatusLogMessageBuilder =
        CommonStatusLogMessageBuilder(responseObjectLoggingEnabled = responseObjectLoggingEnabled, node = node, pod = pod, cluster = cluster, version = version, pinpointAgentId = pinpointAgentId, type = type)

    @Bean
    fun contentCachingWrappingFilter(): ContentCachingWrappingFilter =
        ContentCachingWrappingFilter(enableContentCaching = enableContentCaching, ignorePathPatterns = contentCachingIgnorePathPatterns.map { PathPatternParser.defaultInstance.parse(it) })

    override fun addInterceptors(registry: InterceptorRegistry) {
        // 직접 멤버 변수를 참조하는 대신 context에서 빈을 찾아 넣어주는 구조
    }

    @Configuration
    class StatusLoggingWebMvcConfigurer(
        private val interceptor: StatusLoggingHandlerInterceptor
    ) : WebMvcConfigurer {
        override fun addInterceptors(registry: InterceptorRegistry) {
            registry.addInterceptor(interceptor)
        }
    }
}
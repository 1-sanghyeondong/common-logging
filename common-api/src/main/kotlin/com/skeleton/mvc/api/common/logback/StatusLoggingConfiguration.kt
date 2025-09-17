package com.skeleton.mvc.api.common.logback

import com.skeleton.mvc.api.common.logback.annotations.EnableLogging
import com.skeleton.mvc.api.common.logback.common.utils.LoggingUtil
import com.skeleton.mvc.api.common.logback.status.builder.CommonStatusLogMessageBuilder
import com.skeleton.mvc.api.common.logback.status.builder.StatusLogMessageBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.util.pattern.PathPatternParser
import javax.annotation.PostConstruct

@ConditionalOnBean(annotation = [EnableLogging::class])
@Configuration
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
class StatusLoggingConfiguration(
    private val context: ApplicationContext
) : WebMvcConfigurer {
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

    private lateinit var statusLoggingHandlerInterceptor: StatusLoggingHandlerInterceptor

    @PostConstruct
    fun statusLoggingHandlerInterceptor() {
        val messageBuilder: StatusLogMessageBuilder = context.getBean(StatusLogMessageBuilder::class.java)
        statusLoggingHandlerInterceptor =
            StatusLoggingHandlerInterceptor(
                service = service,
                phase = phase,
                messageBuilder = messageBuilder
            )
    }

    /**
     * Spring BeanFactory에 StatusLogMessageBuilder 빈이 없다면 기본 메시지 빌더를 생성한다.
     *
     * @return [StatusLogMessageBuilder] 기본(공통) 메시지 빌더
     */
    @Bean
    @ConditionalOnMissingBean(StatusLogMessageBuilder::class)
    fun defaultStatusLogMessageBuilder(
        @Value("#{systemEnvironment['NODE_NAME'] ?: '-'}") node: String = LoggingUtil.EMPTY_KEY,
        @Value("#{systemEnvironment['HOSTNAME'] ?: '-'}") pod: String,
        @Value("#{systemEnvironment['CLUSTER'] ?: '-'}") cluster: String,
        @Value("#{systemEnvironment['VERSION'] ?: '-'}") version: String,
        @Value("#{systemEnvironment['PINPOINT_ID'] ?: '-'}") pinpointAgentId: String,
        @Value("#{systemEnvironment['APP_TYPE'] ?: '-'}") type: String
    ): StatusLogMessageBuilder? =
        CommonStatusLogMessageBuilder(
            responseObjectLoggingEnabled = responseObjectLoggingEnabled,
            node = node,
            pod = pod,
            cluster = cluster,
            version = version,
            pinpointAgentId = pinpointAgentId,
            type = type
        )

    @Bean
    fun contentCachingWrappingFilter(): ContentCachingWrappingFilter =
        ContentCachingWrappingFilter(
            enableContentCaching = enableContentCaching,
            ignorePathPatterns = contentCachingIgnorePathPatterns.map { PathPatternParser.defaultInstance.parse(it) }
        )

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(statusLoggingHandlerInterceptor)
    }
}

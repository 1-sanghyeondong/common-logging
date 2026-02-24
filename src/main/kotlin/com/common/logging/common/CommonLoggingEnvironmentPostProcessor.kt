package com.common.logging.common

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class CommonLoggingEnvironmentPostProcessor : EnvironmentPostProcessor, Ordered {
    companion object {
        private const val PROPERTY_SOURCE_NAME = "common-logging-defaults"

        private val DEFAULT_PROPERTIES: Map<String, Any> = mapOf(
            "spring.cloud.config.enabled"              to "false",
            "spring.cloud.config.import-check.enabled" to "false",
        )
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        if (environment.propertySources.contains(PROPERTY_SOURCE_NAME)) return

        // addFirst: 최고 우선순위로 추가 (나머지 PropertySource 들이 덮어쓸 수 없음)
        environment.propertySources.addFirst(
            MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULT_PROPERTIES)
        )
    }
}

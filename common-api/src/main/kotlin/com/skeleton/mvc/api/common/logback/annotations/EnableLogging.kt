package com.skeleton.mvc.api.common.logback.annotations

import com.skeleton.mvc.api.common.logback.StatusLoggingConfiguration
import com.skeleton.mvc.api.common.logback.requestmapping.RequestMappingLoggerConfiguration
import org.springframework.context.annotation.Import

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Import(StatusLoggingConfiguration::class, RequestMappingLoggerConfiguration::class)
annotation class EnableLogging

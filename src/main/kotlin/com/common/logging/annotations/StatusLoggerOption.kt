package com.common.logging.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class StatusLoggerOption(
    val fullBody: Boolean
)

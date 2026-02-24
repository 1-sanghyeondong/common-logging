package com.common.logging.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class StatusLoggerOption(
    val fullBody: Boolean // status log 에 전제 body 노출 유무
)

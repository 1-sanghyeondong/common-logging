package com.common.logging.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class StatusLoggerOption(
    /**
     * status log 에 전제 body 노출 유무
     */
    val fullBody: Boolean
)

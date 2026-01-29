package com.common.logging.annotations

/**
 * prometheus 요청에 대해 로그를 남기지 않기 위한 어노테이션
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class IgnoreProm

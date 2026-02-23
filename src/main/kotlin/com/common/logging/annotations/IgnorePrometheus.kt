package com.common.logging.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class IgnorePrometheus // prometheus 요청에 대해 로그를 남기지 않기 위한 어노테이션

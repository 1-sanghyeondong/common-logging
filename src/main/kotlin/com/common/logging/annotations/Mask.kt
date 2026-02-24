package com.common.logging.annotations

import com.common.logging.common.domain.MaskType

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
)
annotation class Mask(val type: MaskType) // 로그 출력 시 개인정보 마스킹을 적용하는 어노테이션

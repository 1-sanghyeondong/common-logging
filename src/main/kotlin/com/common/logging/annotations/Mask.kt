package com.common.logging.annotations

/**
 * 로그 출력 시 개인정보 마스킹을 적용하는 어노테이션
 *
 * DTO 필드에 선언하면 [LogObjectMapper]가 직렬화할 때 자동으로 마스킹된 값을 출력
 * JSON 트리 순회 경로(에러 응답 등)에서는 [MaskType.fieldNameIndex] 기반으로 필드명을 감지해 마스킹
 *
 * 사용 예:
 * ```kotlin
 * data class UserResponse(
 *     val id: Long,
 *     @Mask(MaskType.NAME)  val name: String,        // 홍길동 → 홍**
 *     @Mask(MaskType.PHONE) val phone: String,       // 010-1234-5678 → 010-****-5678
 *     @Mask(MaskType.EMAIL) val email: String,       // hong@example.com → hon***@example.com
 * )
 * ```
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
)
annotation class Mask(val type: MaskType)

package com.common.logging.annotations

/**
 * 개인정보 마스킹 유형
 *
 * 각 유형은 다음을 정의
 *  - [pattern]          : 마스킹 대상을 식별하는 정규식
 *  - [replacement]      : 정규식 그룹을 이용한 대체 문자열
 *  - [defaultFieldNames]: JSON 트리 순회 시 자동으로 마스킹할 기본 필드명 목록
 *
 * 사용 예:
 * ```kotlin
 * data class UserResponse(
 *     val name: String,
 *     @Mask(type = MaskType.PHONE) val phone: String,
 *     @Mask(type = MaskType.EMAIL) val email: String,
 * )
 * ```
 */
enum class MaskType(
    val pattern: Regex,
    val replacement: String,
    val defaultFieldNames: Set<String> = emptySet(),
) {

    /**
     * 휴대폰 / 전화번호
     * 010-1234-5678  → 010-****-5678
     * 0212345678     → 02-****-5678
     */
    PHONE(
        pattern = Regex("""(\d{2,3})-?(\d{3,4})-?(\d{4})"""),
        replacement = """${'$'}1-****-${'$'}3""",
        defaultFieldNames = setOf(
            "phone", "phoneNumber", "phoneNo",
            "tel", "telNumber", "telNo",
            "mobile", "mobileNumber", "mobileNo",
            "cellphone", "cellPhone"
        ),
    ),

    /**
     * 이메일 주소
     * hong@example.com → hon***@example.com
     */
    EMAIL(
        pattern = Regex("""([^@]{1,3})[^@]*(@.+)"""),
        replacement = """${'$'}1***${'$'}2""",
        defaultFieldNames = setOf(
            "email", "emailAddress", "mail", "mailAddress"
        ),
    ),

    /**
     * 이름 (한국어·영문 공통)
     * 홍길동 → 홍**
     * John  → J***
     */
    NAME(
        pattern = Regex("""(.).+"""),
        replacement = """${'$'}1**""",
        defaultFieldNames = setOf(
            "name", "userName", "fullName", "realName",
            "korName", "engName"
        ),
    ),

    /**
     * 주민등록번호
     * 900101-1234567 → 900101-*******
     */
    SSN(
        pattern = Regex("""(\d{6})-?(\d{7})"""),
        replacement = """${'$'}1-*******""",
        defaultFieldNames = setOf(
            "ssn", "jumin", "juminNo", "rrn",
            "residentNumber", "residentRegistrationNumber",
            "identityNumber"
        ),
    ),

    /**
     * 신용카드 번호
     * 1234-5678-9012-3456 → 1234-****-****-3456
     */
    CARD_NUMBER(
        pattern = Regex("""(\d{4})-?(\d{4})-?(\d{4})-?(\d{4})"""),
        replacement = """${'$'}1-****-****-${'$'}4""",
        defaultFieldNames = setOf(
            "cardNumber", "cardNo", "creditCardNumber",
            "debitCardNumber", "cardNum"
        ),
    ),

    /**
     * 계좌번호 — 은행마다 형식이 달라 중간 자리만 마스킹
     * 123-456789-01-234 → 123-******-01-234
     * (첫 번째 구분자 이후 연속 숫자 마스킹)
     */
    ACCOUNT_NUMBER(
        pattern = Regex("""(\d{3,4})-(\d+)(-\d+)"""),
        replacement = """${'$'}1-******${'$'}3""",
        defaultFieldNames = setOf(
            "accountNumber", "accountNo", "accountNum",
            "bankAccount", "bankAccountNumber"
        ),
    ),

    /**
     * IP 주소
     * 192.168.1.100 → 192.168.*.*
     */
    IP(
        pattern = Regex("""(\d{1,3}\.\d{1,3})\.\d{1,3}\.\d{1,3}"""),
        replacement = """${'$'}1.*.*""",
        defaultFieldNames = setOf(
            "ip", "ipAddress", "clientIp", "userIp",
            "remoteIp", "remoteAddress"
        ),
    ),

    /**
     * 주소 — 상세 주소(읍면동 이하) 마스킹
     * 서울특별시 강남구 역삼동 123-45 → 서울특별시 강남구 ***
     */
    ADDRESS(
        pattern = Regex("""^(.{2,10}(?:특별시|광역시|도)\s*.{1,10}(?:시|군|구)).*$"""),
        replacement = """${'$'}1 ***""",
        defaultFieldNames = setOf(
            "address", "addr", "fullAddress",
            "roadAddress", "jibunAddress", "detailAddress"
        ),
    );

    /**
     * 주어진 문자열에 마스킹 패턴을 적용해 반환한다.
     * 패턴에 매칭되지 않으면 원본 문자열을 그대로 반환한다.
     */
    fun mask(value: String): String = pattern.replace(value, replacement)

    companion object {
        /**
         * 필드명 → MaskType 역방향 조회 맵.
         * [defaultFieldNames] 기준으로 구축되며, JSON 트리 순회 마스킹에 사용된다.
         */
        val fieldNameIndex: Map<String, MaskType> = buildMap {
            values().forEach { type: MaskType ->
                type.defaultFieldNames.forEach { fieldName: String ->
                    put(fieldName, type)
                }
            }
        }
    }
}

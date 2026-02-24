package com.common.logging.common.domain

enum class MaskType(
    val pattern: Regex, // 마스킹 대상을 식별하는 정규식
    val replacement: String, // 정규식 그룹을 이용한 대체 문자열
    val defaultFieldNames: Set<String> = emptySet(), // JSON 트리 순회 시 자동으로 마스킹할 기본 필드명 목록
) {

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

    EMAIL(
        pattern = Regex("""([^@]{1,3})[^@]*(@.+)"""),
        replacement = """${'$'}1***${'$'}2""",
        defaultFieldNames = setOf(
            "email", "emailAddress", "mail", "mailAddress"
        ),
    ),

    NAME(
        pattern = Regex("""(.).+"""),
        replacement = """${'$'}1**""",
        defaultFieldNames = setOf(
            "name", "userName", "fullName", "realName",
            "korName", "engName"
        ),
    ),

    SSN(
        pattern = Regex("""(\d{6})-?(\d{7})"""),
        replacement = """${'$'}1-*******""",
        defaultFieldNames = setOf(
            "ssn", "jumin", "juminNo", "rrn",
            "residentNumber", "residentRegistrationNumber",
            "identityNumber"
        ),
    ),

    CARD_NUMBER(
        pattern = Regex("""(\d{4})-?(\d{4})-?(\d{4})-?(\d{4})"""),
        replacement = """${'$'}1-****-****-${'$'}4""",
        defaultFieldNames = setOf(
            "cardNumber", "cardNo", "creditCardNumber",
            "debitCardNumber", "cardNum"
        ),
    ),

    ACCOUNT_NUMBER(
        pattern = Regex("""(\d{3,4})-(\d+)(-\d+)"""),
        replacement = """${'$'}1-******${'$'}3""",
        defaultFieldNames = setOf(
            "accountNumber", "accountNo", "accountNum",
            "bankAccount", "bankAccountNumber"
        ),
    ),

    IP(
        pattern = Regex("""(\d{1,3}\.\d{1,3})\.\d{1,3}\.\d{1,3}"""),
        replacement = """${'$'}1.*.*""",
        defaultFieldNames = setOf(
            "ip", "ipAddress", "clientIp", "userIp",
            "remoteIp", "remoteAddress"
        ),
    ),

    ADDRESS(
        pattern = Regex("""^(.{2,10}(?:특별시|광역시|도)\s*.{1,10}(?:시|군|구)).*$"""),
        replacement = """${'$'}1 ***""",
        defaultFieldNames = setOf(
            "address", "addr", "fullAddress",
            "roadAddress", "jibunAddress", "detailAddress"
        ),
    );

    fun mask(value: String): String = pattern.replace(value, replacement)

    companion object {
        val fieldNameIndex: Map<String, MaskType> = buildMap {
            MaskType.entries.forEach { type: MaskType ->
                type.defaultFieldNames.forEach { fieldName: String ->
                    put(fieldName, type)
                }
            }
        }
    }
}
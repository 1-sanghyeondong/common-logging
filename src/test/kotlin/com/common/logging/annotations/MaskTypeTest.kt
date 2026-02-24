package com.common.logging.annotations

import com.common.logging.common.domain.MaskType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("MaskType 정규식 마스킹 단위 테스트")
class MaskTypeTest {

    @Nested
    @DisplayName("PHONE — 휴대폰 / 전화번호")
    inner class PhoneTest {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource(
            "010-1234-5678, 010-****-5678",
            "01012345678,   010-****-5678",
            "02-123-4567,   02-****-4567",    // 지역번호
        )
        fun `should mask phone numbers correctly`(input: String, expected: String) {
            assertThat(MaskType.PHONE.mask(input.trim())).isEqualTo(expected.trim())
        }
    }

    @Nested
    @DisplayName("EMAIL — 이메일 주소")
    inner class EmailTest {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource(
            "hong@example.com,        hon***@example.com",
            "ab@test.co.kr,           ab***@test.co.kr",
            "a@domain.com,            a***@domain.com",
        )
        fun `should mask email addresses correctly`(input: String, expected: String) {
            assertThat(MaskType.EMAIL.mask(input.trim())).isEqualTo(expected.trim())
        }
    }

    @Nested
    @DisplayName("NAME — 이름")
    inner class NameTest {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource(
            "홍길동, 홍**",
            "김철수, 김**",
            "John,  J**",
        )
        fun `should mask names correctly`(input: String, expected: String) {
            assertThat(MaskType.NAME.mask(input.trim())).isEqualTo(expected.trim())
        }
    }

    @Nested
    @DisplayName("SSN — 주민등록번호")
    inner class SsnTest {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource(
            "900101-1234567, 900101-*******",
            "9001011234567,  900101-*******",
        )
        fun `should mask SSN correctly`(input: String, expected: String) {
            assertThat(MaskType.SSN.mask(input.trim())).isEqualTo(expected.trim())
        }
    }

    @Nested
    @DisplayName("CARD_NUMBER — 신용카드 번호")
    inner class CardNumberTest {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource(
            "1234-5678-9012-3456, 1234-****-****-3456",
            "1234567890123456,    1234-****-****-3456",
        )
        fun `should mask card numbers correctly`(input: String, expected: String) {
            assertThat(MaskType.CARD_NUMBER.mask(input.trim())).isEqualTo(expected.trim())
        }
    }

    @Nested
    @DisplayName("IP — IP 주소")
    inner class IpTest {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource(
            "192.168.1.100, 192.168.*.*",
            "10.0.0.1,      10.0.*.*",
        )
        fun `should mask IP addresses correctly`(input: String, expected: String) {
            assertThat(MaskType.IP.mask(input.trim())).isEqualTo(expected.trim())
        }
    }

    @Nested
    @DisplayName("ACCOUNT_NUMBER — 계좌번호")
    inner class AccountNumberTest {

        @Test
        @DisplayName("계좌번호 중간 구간이 마스킹된다")
        fun `should mask account number middle part`() {
            val masked = MaskType.ACCOUNT_NUMBER.mask("123-456789-01")
            assertThat(masked).doesNotContain("456789")
            assertThat(masked).contains("123")
            assertThat(masked).contains("01")
        }
    }

    @Nested
    @DisplayName("fieldNameIndex — 기본 필드명 역방향 조회")
    inner class FieldNameIndexTest {

        @ParameterizedTest(name = "'{0}' → MaskType.{1}")
        @CsvSource(
            "phone,       PHONE",
            "phoneNumber, PHONE",
            "mobile,      PHONE",
            "email,       EMAIL",
            "emailAddress,EMAIL",
            "name,        NAME",
            "ssn,         SSN",
            "cardNumber,  CARD_NUMBER",
            "accountNumber,ACCOUNT_NUMBER",
            "ip,          IP",
            "clientIp,    IP",
        )
        fun `field names should resolve to correct MaskType`(fieldName: String, expectedType: String) {
            val maskType = MaskType.fieldNameIndex[fieldName.trim()]
            assertThat(maskType).isNotNull()
            assertThat(maskType!!.name).isEqualTo(expectedType.trim())
        }

        @Test
        @DisplayName("미등록 필드명은 index에 없어야 한다")
        fun `unknown field names should not be in index`() {
            assertThat(MaskType.fieldNameIndex["unknownField"]).isNull()
            assertThat(MaskType.fieldNameIndex["response"]).isNull()
        }
    }
}

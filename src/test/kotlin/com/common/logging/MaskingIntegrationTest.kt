package com.common.logging

import com.common.logging.testutil.InMemoryLogAppender
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/**
 * @Mask 어노테이션 및 JsonMaskUtils 마스킹 통합 테스트.
 *
 * 검증 전략:
 *  - API 응답(HTTP body)에는 원본 값이 그대로 노출되어야 한다.
 *  - STATUS_LOGGER 의 JSON 로그에는 마스킹된 값이 기록되어야 한다.
 *
 * 참고: STATUS_LOGGER 의 responseBody 는
 *   `{"headers":{}, "body":{...실제 DTO...}, "statusCode":"OK"}` 구조로 래핑된다.
 *   따라서 실제 필드는 `.body` 하위에서 꺼내야 한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("개인정보 마스킹 통합 테스트")
class MaskingIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private lateinit var appender: InMemoryLogAppender

    @BeforeEach fun attachAppender() { appender = InMemoryLogAppender.attachTo("STATUS_LOGGER") }
    @AfterEach  fun detachAppender() { appender.detach() }

    // ──────────────────────────────────────────────────────────────
    // 헬퍼: STATUS_LOGGER responseBody 의 body.{field} 에서 값을 꺼낸다
    // ──────────────────────────────────────────────────────────────
    private fun loggedBodyField(fieldName: String): String {
        val log = appender.lastJsonMap()
        val responseBodyJson = log["responseBody"] as? String ?: error("responseBody not found in log")
        val responseNode = objectMapper.readTree(responseBodyJson)
        // 래핑 구조: {headers, body:{…}, statusCode} or 직접 노드
        val bodyNode = if (responseNode.has("body")) responseNode["body"] else responseNode
        return bodyNode[fieldName]?.asText() ?: error("field '$fieldName' not found in logged body")
    }

    // ──────────────────────────────────────────────────────────────
    // 1. @Mask — 정상 응답의 responseBody 마스킹
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /mock/personal-info — @Mask 어노테이션 마스킹")
    inner class PersonalInfoMasking {

        @Test
        @DisplayName("API 응답(HTTP body)에는 원본 값이 그대로 반환된다")
        fun `HTTP response body should contain unmasked original values`() {
            val result = mockMvc.get("/mock/personal-info")
                .andExpect { status { isOk() } }
                .andReturn()

            // MockMvc 의 기본 인코딩이 ISO-8859-1일 수 있으므로 UTF-8 명시
            val body = objectMapper.readTree(result.response.getContentAsString(Charsets.UTF_8))
            assertThat(body["name"].asText()).isEqualTo("홍길동")
            assertThat(body["phone"].asText()).isEqualTo("010-1234-5678")
            assertThat(body["email"].asText()).isEqualTo("hong@example.com")
            assertThat(body["ssn"].asText()).isEqualTo("900101-1234567")
            assertThat(body["cardNumber"].asText()).isEqualTo("1234-5678-9012-3456")
        }

        @Test
        @DisplayName("STATUS_LOGGER 의 responseBody 에는 이름이 마스킹된다")
        fun `STATUS_LOGGER responseBody name should be masked`() {
            mockMvc.get("/mock/personal-info").andExpect { status { isOk() } }
            assertThat(loggedBodyField("name")).isEqualTo("홍**")
        }

        @Test
        @DisplayName("STATUS_LOGGER 의 responseBody 에는 전화번호가 마스킹된다")
        fun `STATUS_LOGGER responseBody phone should be masked`() {
            mockMvc.get("/mock/personal-info").andExpect { status { isOk() } }
            assertThat(loggedBodyField("phone")).isEqualTo("010-****-5678")
        }

        @Test
        @DisplayName("STATUS_LOGGER 의 responseBody 에는 이메일이 마스킹된다")
        fun `STATUS_LOGGER responseBody email should be masked`() {
            mockMvc.get("/mock/personal-info").andExpect { status { isOk() } }
            assertThat(loggedBodyField("email")).isEqualTo("hon***@example.com")
        }

        @Test
        @DisplayName("STATUS_LOGGER 의 responseBody 에는 주민등록번호가 마스킹된다")
        fun `STATUS_LOGGER responseBody SSN should be masked`() {
            mockMvc.get("/mock/personal-info").andExpect { status { isOk() } }
            assertThat(loggedBodyField("ssn")).isEqualTo("900101-*******")
        }

        @Test
        @DisplayName("STATUS_LOGGER 의 responseBody 에는 카드번호가 마스킹된다")
        fun `STATUS_LOGGER responseBody card number should be masked`() {
            mockMvc.get("/mock/personal-info").andExpect { status { isOk() } }
            assertThat(loggedBodyField("cardNumber")).isEqualTo("1234-****-****-3456")
        }

        @Test
        @DisplayName("원본 개인정보가 로그 전체에서 노출되지 않는다")
        fun `original personal info should NOT appear anywhere in STATUS_LOGGER output`() {
            mockMvc.get("/mock/personal-info").andExpect { status { isOk() } }

            val rawLog = appender.lastMessage()
            assertThat(rawLog).doesNotContain("010-1234-5678")
            assertThat(rawLog).doesNotContain("hong@example.com")
            assertThat(rawLog).doesNotContain("900101-1234567")
            assertThat(rawLog).doesNotContain("1234-5678-9012-3456")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 2. @Mask 가 없는 필드는 원본 유지
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("@Mask 가 없는 필드는 로그에 원본 값이 유지된다")
    inner class UnmaskedFieldsPreserved {

        @Test
        @DisplayName("@Mask 미선언 필드(userId 등)는 마스킹되지 않는다")
        fun `fields without @Mask should appear unmasked in log`() {
            mockMvc.get("/mock/personal-info").andExpect { status { isOk() } }

            val log = appender.lastJsonMap()
            val responseBodyJson = log["responseBody"] as? String ?: error("responseBody not found")
            val responseNode = objectMapper.readTree(responseBodyJson)
            val bodyNode = if (responseNode.has("body")) responseNode["body"] else responseNode

            // userId 는 @Mask 없음 → 원본 그대로
            assertThat(bodyNode["userId"].asLong()).isEqualTo(1L)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 3. JsonMaskUtils — 에러 요청 body에서 필드명 기반 마스킹
    // ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("에러 요청 body 의 개인정보 필드명 기반 마스킹")
    inner class ErrorRequestMasking {

        @Test
        @DisplayName("요청 body 에 email 필드가 있으면 failover 로그에 마스킹된다")
        fun `email in request body should be masked in failover log`() {
            // name 없이 email만 → 400 validation error → failover request 로그 경로
            val body = mapOf("email" to "secret@test.com")

            mockMvc.post("/mock/users") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect { status { isBadRequest() } }

            assertThat(appender.hasEvents()).isTrue()
            val rawLog = appender.lastMessage()
            // 원본 이메일이 로그에 노출되지 않아야 한다
            assertThat(rawLog).doesNotContain("secret@test.com")
        }
    }
}

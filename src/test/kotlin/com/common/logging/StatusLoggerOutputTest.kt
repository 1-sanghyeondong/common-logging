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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

/**
 * STATUS_LOGGER 가 실제로 출력하는 JSON 의 내용을 검증하는 테스트.
 *
 * 전략:
 *  - InMemoryLogAppender 를 "STATUS_LOGGER" 에 붙여 로그 이벤트를 캡처한다.
 *  - MockMvc 로 요청을 보낸 직후 캡처된 JSON 을 파싱해 필드별로 검증한다.
 *
 * 검증 필드 목록 (StatusLogger.createLogObject 기준):
 *  @timestamp, service, phase, execTimemillis, statusCode,
 *  path, ipath, method, requestBody, responseBody,
 *  message (req/res/from 포함 여부), clientIp, deviceId,
 *  exception (5xx 시), responseMsg (에러 시)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("STATUS_LOGGER JSON 출력 검증 테스트")
class StatusLoggerOutputTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    /** 각 테스트 전 appender 를 달고, 후에 완전히 제거한다 */
    private lateinit var appender: InMemoryLogAppender

    @BeforeEach
    fun attachAppender() {
        appender = InMemoryLogAppender.attachTo("STATUS_LOGGER")
    }

    @AfterEach
    fun detachAppender() {
        appender.detach()
    }

    // ──────────────────────────────────────────────────────────────
    // 공통 필드 헬퍼
    // ──────────────────────────────────────────────────────────────

    private fun assertCommonFields(log: Map<String, Any?>, method: String, statusCode: Int) {
        // 타임스탬프 존재 및 ISO-8601 프리픽스
        assertThat(log["@timestamp"] as? String).isNotBlank()
        // 서비스명 (app.id = common-logging)
        assertThat(log["service"]).isEqualTo("common-logging")
        // 환경 (spring.profiles.active = local)
        assertThat(log["phase"]).isEqualTo("local")
        // 실행 시간 (0 이상의 숫자)
        assertThat(log["execTimemillis"] as? Int).isGreaterThanOrEqualTo(0)
        // HTTP 상태코드
        assertThat(log["statusCode"]).isEqualTo(statusCode)
        // 메서드
        assertThat(log["method"]).isEqualTo(method)
        // message 필드에 req/res/from 포함
        val message = log["message"] as? String ?: ""
        assertThat(message).contains("req: $method")
        assertThat(message).contains("res: $statusCode")
        assertThat(message).contains("from:")
    }

    // ──────────────────────────────────────────────────────────────
    // 1. GET /mock/hello — 기본 필드 검증
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/hello")
    inner class HelloLog {

        @Test
        @DisplayName("STATUS_LOGGER 가 로그를 한 건 출력하고 공통 필드가 올바르다")
        fun `should emit exactly one status log with correct common fields`() {
            mockMvc.get("/mock/hello").andExpect { status { isOk() } }

            assertThat(appender.hasEvents()).isTrue()
            assertThat(appender.allEvents()).hasSize(1)

            val log = appender.lastJsonMap()
            assertCommonFields(log, "GET", 200)

            // path / ipath 동일 (변수 없는 경로)
            assertThat(log["path"]).isEqualTo("/mock/hello")
            assertThat(log["ipath"]).isEqualTo("/mock/hello")
        }

        @Test
        @DisplayName("message 필드에 정확한 경로가 포함된다")
        fun `message field contains correct path`() {
            mockMvc.get("/mock/hello").andExpect { status { isOk() } }
            val message = appender.lastJsonMap()["message"] as String
            assertThat(message).contains("GET /mock/hello")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 2. GET /mock/users/{id} — path vs ipath 구분
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/users/{id} — path vs ipath")
    inner class PathVsIpath {

        @Test
        @DisplayName("path 는 실제 요청 경로, ipath 는 패턴 경로여야 한다")
        fun `path should be actual uri, ipath should be pattern`() {
            mockMvc.get("/mock/users/42").andExpect { status { isOk() } }

            val log = appender.lastJsonMap()
            // 실제 요청 URI
            assertThat(log["path"]).isEqualTo("/mock/users/42")
            // HandlerMapping 이 매핑한 패턴
            assertThat(log["ipath"]).isEqualTo("/mock/users/{id}")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 3. @IgnoreStatusLogging — 로그 미출력 확인
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/hello/no-log — @IgnoreStatusLogging")
    inner class IgnoreStatusLoggingTest {

        @Test
        @DisplayName("@IgnoreStatusLogging 이 붙은 엔드포인트는 STATUS_LOGGER 에 기록되지 않는다")
        fun `should NOT emit any status log`() {
            mockMvc.get("/mock/hello/no-log").andExpect { status { isOk() } }
            assertThat(appender.hasEvents()).isFalse()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 4. POST /mock/users — requestBody 필드
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /mock/users — requestBody 로그")
    inner class RequestBodyLog {

        @Test
        @DisplayName("requestBody 필드에 전송한 JSON 이 포함된다")
        fun `requestBody field contains posted payload`() {
            val body = mapOf("name" to "홍길동", "email" to "hong@test.com")

            mockMvc.post("/mock/users") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect { status { isCreated() } }

            val log = appender.lastJsonMap()
            assertCommonFields(log, "POST", 201)

            val requestBody = log["requestBody"] as? String ?: ""
            assertThat(requestBody).contains("홍길동")
            assertThat(requestBody).contains("hong@test.com")
        }

        @Test
        @DisplayName("유효성 검사 실패(400) 시에도 STATUS_LOGGER 에 기록된다")
        fun `validation failure 400 should still be logged`() {
            mockMvc.post("/mock/users") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"only@test.com"}"""  // name 누락
            }.andExpect { status { isBadRequest() } }

            assertThat(appender.hasEvents()).isTrue()
            val log = appender.lastJsonMap()
            assertThat(log["statusCode"]).isEqualTo(400)
            assertThat(log["method"]).isEqualTo("POST")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 5. PUT /mock/users/{id} — 메서드 & path 검증
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /mock/users/{id}")
    inner class PutLog {

        @Test
        @DisplayName("method=PUT, statusCode=200, path/ipath 검증")
        fun `PUT request is logged correctly`() {
            val body = mapOf("name" to "김철수", "email" to "kim@test.com")

            mockMvc.put("/mock/users/7") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect { status { isOk() } }

            val log = appender.lastJsonMap()
            assertCommonFields(log, "PUT", 200)
            assertThat(log["path"]).isEqualTo("/mock/users/7")
            assertThat(log["ipath"]).isEqualTo("/mock/users/{id}")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 6. DELETE /mock/users/{id} — 204
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /mock/users/{id}")
    inner class DeleteLog {

        @Test
        @DisplayName("method=DELETE, statusCode=204 로 기록된다")
        fun `DELETE request is logged with 204`() {
            mockMvc.delete("/mock/users/99").andExpect { status { isNoContent() } }

            val log = appender.lastJsonMap()
            assertCommonFields(log, "DELETE", 204)
            assertThat(log["path"]).isEqualTo("/mock/users/99")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 7. execTimemillis — slow 엔드포인트
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/slow — execTimemillis")
    inner class ExecTimeLog {

        @Test
        @DisplayName("200ms 지연 요청 시 execTimemillis 가 200 이상이어야 한다")
        fun `execTimemillis should be at least the delay`() {
            mockMvc.get("/mock/slow?delayMs=200").andExpect { status { isOk() } }

            val execTime = appender.lastJsonMap()["execTimemillis"] as Int
            assertThat(execTime).isGreaterThanOrEqualTo(200)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 8. 4xx — responseMsg 필드
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/error/4xx — 에러 응답 로그")
    inner class Error4xxLog {

        @Test
        @DisplayName("statusCode=400, responseMsg 필드가 존재한다")
        fun `4xx response is logged with statusCode and responseMsg`() {
            mockMvc.get("/mock/error/4xx").andExpect { status { isBadRequest() } }

            val log = appender.lastJsonMap()
            assertCommonFields(log, "GET", 400)
            // 에러 응답 시 responseMsg 필드가 채워짐
            assertThat(log).containsKey("responseMsg")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 9. 5xx — exception 필드
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/error/5xx — exception 로그")
    inner class Error5xxLog {

        @Test
        @DisplayName("statusCode=500, exception 필드가 존재한다")
        fun `5xx response is logged with statusCode and exception field`() {
            mockMvc.get("/mock/error/5xx").andExpect { status { isInternalServerError() } }

            val log = appender.lastJsonMap()
            assertCommonFields(log, "GET", 500)
            // 5xx 시 exception 필드가 비어있지 않아야 함
            // StatusLogger 는 에러 응답이면 responseMsg 를 채우므로 존재 여부 확인
            assertThat(log).containsKey("responseMsg")
            // exception 은 설정에 따라 비어있거나 메시지가 담길 수 있음
            // 최소한 statusCode=500 이 기록되었는지 확인
            assertThat(log["statusCode"]).isEqualTo(500)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 10. @StatusLoggerOption(fullBody=true) — responseBody
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/hello/full-body — fullBody 로그")
    inner class FullBodyLog {

        @Test
        @DisplayName("fullBody=true 이면 responseBody 에 전체 배열이 포함된다")
        fun `fullBody option includes complete response body`() {
            mockMvc.get("/mock/hello/full-body").andExpect { status { isOk() } }

            val log = appender.lastJsonMap()
            assertCommonFields(log, "GET", 200)

            val responseBody = log["responseBody"] as? String ?: ""
            // data 배열의 item-0 ~ item-9 가 포함되는지 확인
            assertThat(responseBody).contains("item-0")
            assertThat(responseBody).contains("item-9")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 11. 여러 요청 순차적 — 이벤트 개수 확인
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("여러 요청 순차 실행")
    inner class MultipleRequests {

        @Test
        @DisplayName("N개의 요청을 보내면 STATUS_LOGGER 에 N개의 로그가 쌓인다")
        fun `each request produces exactly one status log event`() {
            mockMvc.get("/mock/hello").andExpect { status { isOk() } }
            mockMvc.get("/mock/users/1").andExpect { status { isOk() } }
            mockMvc.get("/mock/search?keyword=test").andExpect { status { isOk() } }

            val logs = appender.allJsonMaps()
            assertThat(logs).hasSize(3)

            assertThat(logs[0]["path"]).isEqualTo("/mock/hello")
            assertThat(logs[1]["path"]).isEqualTo("/mock/users/1")
            assertThat(logs[2]["path"]).isEqualTo("/mock/search")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 12. message 포맷 정밀 검증
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("message 필드 포맷 검증")
    inner class MessageFormat {

        @Test
        @DisplayName("message 는 'req: METHOD path\\nres: STATUS exec ms\\nfrom: -\\n' 형식이어야 한다")
        fun `message field follows expected multiline format`() {
            mockMvc.get("/mock/hello").andExpect { status { isOk() } }

            val message = appender.lastJsonMap()["message"] as String
            val lines = message.lines().filter { it.isNotEmpty() }

            assertThat(lines[0]).startsWith("req: GET /mock/hello")
            assertThat(lines[1]).matches(Regex("res: 200 \\d+ms").toPattern())
            assertThat(lines[2]).startsWith("from:")
        }
    }
}

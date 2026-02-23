package com.common.logging

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("MockApiController 통합 테스트")
class MockApiControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    // ──────────────────────────────────────────────────────────────
    // 1. GET /mock/hello
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/hello - 기본 로그 확인")
    inner class Hello {

        @Test
        @DisplayName("200 응답 반환 및 message, timestamp 필드 존재")
        fun `should return 200 with message and timestamp`() {
            mockMvc.get("/mock/hello")
                .andExpect {
                    status { isOk() }
                    content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                    jsonPath("$.message") { value("Hello, Logging World!") }
                    jsonPath("$.timestamp") { value(notNullValue()) }
                }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 2. GET /mock/hello/full-body
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/hello/full-body - fullBody 로그 확인")
    inner class HelloFullBody {

        @Test
        @DisplayName("200 응답 반환 및 data 배열 10개 포함")
        fun `should return 200 with full body including data array`() {
            mockMvc.get("/mock/hello/full-body")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.message") { value("This response body will be fully logged") }
                    jsonPath("$.data") { isArray() }
                    jsonPath("$.data", hasSize<Any>(10))
                    jsonPath("$.timestamp") { value(notNullValue()) }
                }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 3. GET /mock/hello/no-log
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/hello/no-log - @IgnoreStatusLogging 확인")
    inner class HelloNoLog {

        @Test
        @DisplayName("200 응답 반환 (status log 제외 엔드포인트)")
        fun `should return 200 with exclusion message`() {
            mockMvc.get("/mock/hello/no-log")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.message") { value("This endpoint is excluded from status logging") }
                }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 4. GET /mock/users/{id}
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/users/{id} - path variable 확인")
    inner class GetUser {

        @Test
        @DisplayName("존재하는 id로 요청 시 사용자 정보 반환")
        fun `should return user with matching id`() {
            mockMvc.get("/mock/users/42")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.id") { value(42) }
                    jsonPath("$.name") { value("Test User 42") }
                    jsonPath("$.email") { value("user42@example.com") }
                    jsonPath("$.createdAt") { value(notNullValue()) }
                }
        }

        @Test
        @DisplayName("다른 id로 요청해도 각각 맞는 사용자 정보 반환")
        fun `should return different users for different ids`() {
            mockMvc.get("/mock/users/100")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.id") { value(100) }
                    jsonPath("$.name") { value("Test User 100") }
                    jsonPath("$.email") { value("user100@example.com") }
                }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 5. GET /mock/search
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/search - query param 확인")
    inner class Search {

        @Test
        @DisplayName("keyword, page, size 파라미터가 응답에 반영됨")
        fun `should reflect query params in response`() {
            mockMvc.get("/mock/search?keyword=hello&page=2&size=5")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.keyword") { value("hello") }
                    jsonPath("$.page") { value(2) }
                    jsonPath("$.size") { value(5) }
                    jsonPath("$.totalCount") { value(42) }
                    jsonPath("$.items") { isArray() }
                    jsonPath("$.items", hasSize<Any>(5))
                    jsonPath("$.items[0]") { value(containsString("hello")) }
                }
        }

        @Test
        @DisplayName("파라미터 없이 호출 시 기본값 사용")
        fun `should use default values when no params provided`() {
            mockMvc.get("/mock/search")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.keyword") { value("") }
                    jsonPath("$.page") { value(1) }
                    jsonPath("$.size") { value(20) }
                }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 6. POST /mock/users
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /mock/users - request body 로그 확인")
    inner class CreateUser {

        @Test
        @DisplayName("유효한 body 전송 시 201 응답과 생성된 사용자 반환")
        fun `should return 201 with created user`() {
            val body = mapOf("name" to "홍길동", "email" to "hong@test.com")

            mockMvc.post("/mock/users") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(notNullValue()) }
                jsonPath("$.name") { value("홍길동") }
                jsonPath("$.email") { value("hong@test.com") }
                jsonPath("$.createdAt") { value(notNullValue()) }
            }
        }

        @Test
        @DisplayName("이름 누락 시 400 응답 반환")
        fun `should return 400 when name is missing`() {
            val body = mapOf("email" to "hong@test.com")

            mockMvc.post("/mock/users") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("VALIDATION_FAILED") }
                jsonPath("$.message") { value(containsString("name")) }
            }
        }

        @Test
        @DisplayName("이름이 1자 이하이면 400 응답 반환 (@Size 검증)")
        fun `should return 400 when name is too short`() {
            val body = mapOf("name" to "A", "email" to "hong@test.com")

            mockMvc.post("/mock/users") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("VALIDATION_FAILED") }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 7. PUT /mock/users/{id}
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /mock/users/{id} - 업데이트 확인")
    inner class UpdateUser {

        @Test
        @DisplayName("유효한 body와 id로 요청 시 200 반환")
        fun `should return 200 with updated user`() {
            val body = mapOf("name" to "김철수", "email" to "kim@test.com")

            mockMvc.put("/mock/users/7") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value(7) }
                jsonPath("$.name") { value("김철수") }
                jsonPath("$.email") { value("kim@test.com") }
                jsonPath("$.createdAt") { value(notNullValue()) }
            }
        }

        @Test
        @DisplayName("email 누락 시 400 응답")
        fun `should return 400 when email is missing`() {
            val body = mapOf("name" to "김철수")

            mockMvc.put("/mock/users/7") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("VALIDATION_FAILED") }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 8. DELETE /mock/users/{id}
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /mock/users/{id} - 204 No Content 확인")
    inner class DeleteUser {

        @Test
        @DisplayName("204 응답이며 body가 없어야 함")
        fun `should return 204 with no content`() {
            mockMvc.delete("/mock/users/99")
                .andExpect {
                    status { isNoContent() }
                }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 9. GET /mock/slow
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/slow - execTimemillis 확인")
    inner class SlowEndpoint {

        @Test
        @DisplayName("200 응답과 실제 지연 시간이 반영된 delayMs 반환")
        fun `should return 200 with delay info`() {
            mockMvc.get("/mock/slow?delayMs=200")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.message") { value("Delayed response") }
                    jsonPath("$.delayMs") { value(200) }
                }
        }

        @Test
        @DisplayName("delayMs가 3000 초과이면 3000으로 clamp됨")
        fun `should clamp delayMs to 3000`() {
            mockMvc.get("/mock/slow?delayMs=99999")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.delayMs") { value(3000) }
                }
        }

        @Test
        @DisplayName("delayMs 없이 호출하면 기본값 500ms 적용")
        fun `should use default 500ms delay`() {
            mockMvc.get("/mock/slow")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.delayMs") { value(500) }
                }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 10. GET /mock/error/4xx
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/error/4xx - 4xx 에러 로그 확인")
    inner class Error4xx {

        @Test
        @DisplayName("400 응답 반환 및 code, message 필드 존재")
        fun `should return 400 with error body`() {
            mockMvc.get("/mock/error/4xx")
                .andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("INVALID_REQUEST") }
                    jsonPath("$.message") { value("This is a simulated 400 error") }
                }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 11. GET /mock/error/5xx
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /mock/error/5xx - exception 로그 확인")
    inner class Error5xx {

        @Test
        @DisplayName("500 응답 반환 및 GlobalExceptionHandler가 처리")
        fun `should return 500 handled by global exception handler`() {
            mockMvc.get("/mock/error/5xx")
                .andExpect {
                    status { isInternalServerError() }
                    jsonPath("$.code") { value("INTERNAL_SERVER_ERROR") }
                    jsonPath("$.message") { value("Simulated internal server error for logging test") }
                    jsonPath("$.timestamp") { value(notNullValue()) }
                }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 12. POST /mock/echo
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /mock/echo - body truncation 확인")
    inner class Echo {

        @Test
        @DisplayName("요청 body가 그대로 반환됨")
        fun `should return same body as request`() {
            val body = mapOf(
                "key1" to "value1",
                "key2" to 42,
                "key3" to listOf("a", "b", "c")
            )

            mockMvc.post("/mock/echo") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isOk() }
                jsonPath("$.key1") { value("value1") }
                jsonPath("$.key2") { value(42) }
                jsonPath("$.key3") { isArray() }
                jsonPath("$.key3", hasSize<Any>(3))
            }
        }

        @Test
        @DisplayName("대용량 body도 그대로 반환됨 (truncation 테스트)")
        fun `should return large body as-is`() {
            val largeBody = mapOf(
                "payload" to "x".repeat(5000),
                "count" to 9999
            )

            mockMvc.post("/mock/echo") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(largeBody)
            }.andExpect {
                status { isOk() }
                jsonPath("$.payload") { value("x".repeat(5000)) }
                jsonPath("$.count") { value(greaterThanOrEqualTo(9999)) }
            }
        }
    }
}

package com.common.logging

import com.common.logging.annotations.IgnoreStatusLogging
import com.common.logging.annotations.Mask
import com.common.logging.annotations.MaskType
import com.common.logging.annotations.StatusLoggerOption
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/mock")
class MockApiController {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // ──────────────────────────────────────────────────────────────
    // 1. 기본 GET – STATUS_LOGGER 로그 확인
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/hello")
    fun hello(): ResponseEntity<Map<String, Any>> {
        logger.debug("hello endpoint called")
        val body = mapOf(
            "message" to "Hello, Logging World!",
            "timestamp" to LocalDateTime.now().toString()
        )
        return ResponseEntity.ok(body)
    }

    // ──────────────────────────────────────────────────────────────
    // 2. fullBody=true – responseBody 전체 노출 확인
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/hello/full-body")
    @StatusLoggerOption(fullBody = true)
    fun helloFullBody(): ResponseEntity<Map<String, Any>> {
        val body = mapOf(
            "message" to "This response body will be fully logged",
            "data" to List(10) { "item-$it" },
            "timestamp" to LocalDateTime.now().toString()
        )
        return ResponseEntity.ok(body)
    }

    // ──────────────────────────────────────────────────────────────
    // 3. @IgnoreStatusLogging – STATUS_LOGGER 에 기록되지 않음
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/hello/no-log")
    @IgnoreStatusLogging
    fun helloNoLog(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("message" to "This endpoint is excluded from status logging"))
    }

    // ──────────────────────────────────────────────────────────────
    // 4. Path variable – ipath / path 필드 차이 확인
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/users/{id}")
    fun getUser(@PathVariable id: Long): ResponseEntity<UserResponse> {
        val user = UserResponse(
            id = id,
            name = "Test User $id",
            email = "user$id@example.com",
            createdAt = LocalDateTime.now().toString()
        )
        return ResponseEntity.ok(user)
    }

    // ──────────────────────────────────────────────────────────────
    // 5. Query param – path에 query string 포함 확인
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/search")
    fun search(
        @RequestParam(defaultValue = "") keyword: String,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Map<String, Any>> {
        val result = mapOf(
            "keyword" to keyword,
            "page" to page,
            "size" to size,
            "totalCount" to 42,
            "items" to List(minOf(size, 5)) { "result-$it: $keyword" }
        )
        return ResponseEntity.ok(result)
    }

    // ──────────────────────────────────────────────────────────────
    // 6. POST – requestBody 로그 확인
    // ──────────────────────────────────────────────────────────────

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        logger.info("Creating user: name={}", request.name)
        val created = UserResponse(
            id = (1000..9999).random().toLong(),
            name = request.name!!,
            email = request.email!!,
            createdAt = LocalDateTime.now().toString()
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    // ──────────────────────────────────────────────────────────────
    // 7. PUT – path variable + body 업데이트
    // ──────────────────────────────────────────────────────────────

    @PutMapping("/users/{id}")
    fun updateUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: CreateUserRequest
    ): ResponseEntity<UserResponse> {
        val updated = UserResponse(
            id = id,
            name = request.name!!,
            email = request.email!!,
            createdAt = LocalDateTime.now().toString()
        )
        return ResponseEntity.ok(updated)
    }

    // ──────────────────────────────────────────────────────────────
    // 8. DELETE – 204 No Content
    // ──────────────────────────────────────────────────────────────

    @DeleteMapping("/users/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Void> {
        logger.info("Deleting user id={}", id)
        return ResponseEntity.noContent().build()
    }

    // ──────────────────────────────────────────────────────────────
    // 9. Slow endpoint – execTimemillis 확인
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/slow")
    fun slow(@RequestParam(defaultValue = "500") delayMs: Long): ResponseEntity<Map<String, Any>> {
        val clamped = delayMs.coerceIn(0, 3000)
        Thread.sleep(clamped)
        return ResponseEntity.ok(
            mapOf(
                "message" to "Delayed response",
                "delayMs" to clamped
            )
        )
    }

    // ──────────────────────────────────────────────────────────────
    // 10. 4xx 에러 – statusCode 로그 확인
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/error/4xx")
    fun error4xx(): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(code = "INVALID_REQUEST", message = "This is a simulated 400 error"))
    }

    // ──────────────────────────────────────────────────────────────
    // 11. 5xx 에러 – exception 로그 확인
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/error/5xx")
    fun error5xx(): ResponseEntity<ErrorResponse> {
        throw RuntimeException("Simulated internal server error for logging test")
    }

    // ──────────────────────────────────────────────────────────────
    // 12. Echo – 큰 body의 truncation 확인
    // ──────────────────────────────────────────────────────────────

    @PostMapping("/echo")
    fun echo(@RequestBody body: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(body)
    }

    // ──────────────────────────────────────────────────────────────
    // 13. @Mask — 개인정보 마스킹 확인
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/personal-info")
    fun getPersonalInfo(): ResponseEntity<PersonalInfoResponse> {
        val response = PersonalInfoResponse(
            userId   = 1L,
            name     = "홍길동",
            phone    = "010-1234-5678",
            email    = "hong@example.com",
            ssn      = "900101-1234567",
            cardNumber = "1234-5678-9012-3456",
            address  = "서울특별시 강남구 역삼동 123-45"
        )
        return ResponseEntity.ok(response)
    }

    // ──────────────────────────────────────────────────────────────
    // DTOs
    // ──────────────────────────────────────────────────────────────

    /** 로그에는 마스킹된 값이 기록된다. API 응답은 원본 값 그대로 반환한다. */
    data class PersonalInfoResponse(
        val userId: Long,
        @param:Mask(type = MaskType.NAME) val name: String,               // 홍길동 → 홍**
        @param:Mask(type = MaskType.PHONE)       val phone: String,       // 010-1234-5678 → 010-****-5678
        @param:Mask(type = MaskType.EMAIL)       val email: String,       // hong@example.com → hon***@example.com
        @param:Mask(type = MaskType.SSN)         val ssn: String,         // 900101-1234567 → 900101-*******
        @param:Mask(type = MaskType.CARD_NUMBER) val cardNumber: String,  // 1234-5678-9012-3456 → 1234-****-****-3456
        @param:Mask(type = MaskType.ADDRESS)     val address: String,     // 시/구까지만 노출
    )

    data class UserResponse(
        val id: Long,
        val name: String,
        val email: String,
        val createdAt: String
    )

    data class CreateUserRequest(
        @field:NotBlank(message = "name은 필수입니다")
        @field:Size(min = 2, max = 50)
        val name: String? = null,

        @field:NotBlank(message = "email은 필수입니다")
        val email: String? = null
    )

    data class ErrorResponse(
        val code: String,
        val message: String,
        val timestamp: String = LocalDateTime.now().toString()
    )
}

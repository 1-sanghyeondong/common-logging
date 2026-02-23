package com.common.logging

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice(basePackageClasses = [MockApiController::class])
class MockGlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<ErrorBody> {
        logger.error("Unhandled RuntimeException: {}", ex.message, ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorBody(
                    code = "INTERNAL_SERVER_ERROR",
                    message = ex.message ?: "Unexpected error occurred",
                    timestamp = LocalDateTime.now().toString()
                )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorBody> {
        val errors = ex.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorBody(
                    code = "VALIDATION_FAILED",
                    message = errors,
                    timestamp = LocalDateTime.now().toString()
                )
            )
    }

    data class ErrorBody(
        val code: String,
        val message: String,
        val timestamp: String
    )
}

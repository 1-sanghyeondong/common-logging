package com.common.logging

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SystemController {
    @GetMapping("ping")
    fun ping(): String {
        return "pong"
    }
}

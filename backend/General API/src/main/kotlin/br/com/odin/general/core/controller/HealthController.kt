package br.com.odin.general.core.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1/health")
class HealthController {
    @GetMapping
    fun health(): Map<String, Any> =
        mapOf(
            "status" to "UP",
            "service" to "odin-general-api",
            "timestamp" to Instant.now().toString(),
        )
}

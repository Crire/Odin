package br.com.odin.general.identity.service

import br.com.odin.general.identity.domain.AppUser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class JwtTokenService(
    private val objectMapper: ObjectMapper,
    @Value("\${odin.security.jwt-secret}") private val secret: String,
    @Value("\${odin.security.access-token-minutes}") private val accessTokenMinutes: Long,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun createAccessToken(user: AppUser): TokenEnvelope {
        val expiresAt = Instant.now(clock).plus(accessTokenMinutes, ChronoUnit.MINUTES)
        val header = mapOf("alg" to "HS256", "typ" to "JWT")
        val payload = mapOf(
            "sub" to user.id.toString(),
            "email" to user.email,
            "role" to user.role.name,
            "exp" to expiresAt.epochSecond,
        )
        val unsigned = "${encode(header)}.${encode(payload)}"
        return TokenEnvelope(token = "$unsigned.${sign(unsigned)}", expiresAt = expiresAt)
    }

    fun parse(token: String): TokenClaims? {
        val parts = token.split(".")
        if (parts.size != 3) return null
        val unsigned = "${parts[0]}.${parts[1]}"
        val expected = sign(unsigned)
        if (!MessageDigest.isEqual(expected.toByteArray(), parts[2].toByteArray())) return null

        val payloadJson = String(decoder.decode(parts[1]), StandardCharsets.UTF_8)
        val payload = objectMapper.readValue(payloadJson, object : TypeReference<Map<String, Any>>() {})
        val expiresAt = Instant.ofEpochSecond(payload["exp"].toString().toLong())
        if (expiresAt.isBefore(Instant.now(clock))) return null

        return TokenClaims(
            subject = payload["sub"].toString(),
            email = payload["email"].toString(),
            role = payload["role"].toString(),
            expiresAt = expiresAt,
        )
    }

    private fun encode(value: Any): String =
        encoder.encodeToString(objectMapper.writeValueAsBytes(value))

    private fun sign(value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val key = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        mac.init(key)
        return encoder.encodeToString(mac.doFinal(value.toByteArray(StandardCharsets.UTF_8)))
    }

    companion object {
        private val encoder = Base64.getUrlEncoder().withoutPadding()
        private val decoder = Base64.getUrlDecoder()
    }
}

data class TokenEnvelope(
    val token: String,
    val expiresAt: Instant,
)

data class TokenClaims(
    val subject: String,
    val email: String,
    val role: String,
    val expiresAt: Instant,
)

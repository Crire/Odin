package br.com.odin.general.identity.service

import br.com.odin.general.identity.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class JwtAuthenticationFilter(
    private val jwtTokenService: JwtTokenService,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.servletPath == "/api/v1/auth/register" ||
            request.servletPath == "/api/v1/auth/login" ||
            request.servletPath == "/api/v1/health" ||
            request.servletPath.startsWith("/actuator") ||
            request.servletPath.startsWith("/v3/api-docs") ||
            request.servletPath.startsWith("/swagger-ui")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authorization = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authorization?.startsWith("Bearer ") == true && SecurityContextHolder.getContext().authentication == null) {
            authenticate(authorization.removePrefix("Bearer ").trim())
        }
        filterChain.doFilter(request, response)
    }

    private fun authenticate(token: String) {
        val claims = jwtTokenService.parse(token) ?: return
        val user = userRepository.findById(UUID.fromString(claims.subject)).orElse(null) ?: return
        if (!user.active) return

        val currentUser = CurrentUser.from(user)
        val authentication = UsernamePasswordAuthenticationToken(
            currentUser,
            null,
            listOf(SimpleGrantedAuthority("ROLE_${user.role.name}")),
        )
        SecurityContextHolder.getContext().authentication = authentication
    }
}

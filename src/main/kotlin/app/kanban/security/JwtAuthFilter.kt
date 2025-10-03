package app.kanban.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtDecoder: org.springframework.security.oauth2.jwt.JwtDecoder
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val session = request.getSession(false)
        if (session != null && !session.isNew && SecurityContextHolder.getContext().authentication == null) {
            val token = extractJwtFromCookie(request)
            if (!token.isNullOrBlank()) {
                try {
                    val jwt = jwtDecoder.decode(token)
                    val sessionIdOnClaim = jwt.getClaimAsString("sessionId")
                    if (request.session.id == sessionIdOnClaim) {
                        val auth = object : AbstractAuthenticationToken(listOf<GrantedAuthority>()) {
                            override fun getCredentials() = null
                            override fun getPrincipal() = jwt.subject
                        }.apply { isAuthenticated = true }
                        auth.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = auth
                    }
                } catch (ex: Exception) {
                    log.debug("JWT decoding failed: {}", ex.message)
                }
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun extractJwtFromCookie(request: HttpServletRequest): String? {
        val cookies = request.cookies ?: return null
        return cookies.firstOrNull { it.name == "AUTH_TOKEN" }?.value
    }
}

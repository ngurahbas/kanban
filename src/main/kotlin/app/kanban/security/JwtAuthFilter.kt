package app.kanban.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
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
        if (SecurityContextHolder.getContext().authentication == null) {
            val token = extractJwtFromCookie(request)
            if (!token.isNullOrBlank()) {
                try {
                    val jwt = jwtDecoder.decode(token)
                    val authoritiesClaim = jwt.claims["authorities"]
                    val authorities = when (authoritiesClaim) {
                        is Collection<*> -> authoritiesClaim.mapNotNull { it?.toString() }
                        is String -> authoritiesClaim.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                        else -> emptyList()
                    }.map { SimpleGrantedAuthority(it) }
                    val auth = object : AbstractAuthenticationToken(authorities) {
                        override fun getCredentials() = null
                        override fun getPrincipal() = jwt.subject
                    }.apply { isAuthenticated = true }
                    auth.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = auth
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

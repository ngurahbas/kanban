package app.kanban.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    private val oAuth2LoginSuccessHandler: OAuth2LoginSuccessHandler
) {

    private val log = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/kanban/**")//TODO make csrf works
            }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/login", "/oauth2/**", "/css/**", "/js/**").permitAll()
                    .anyRequest().authenticated()
            }.oauth2Login { oauth2Login ->
                oauth2Login.successHandler(oAuth2LoginSuccessHandler)
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .logout {
                it.clearAuthentication(true).invalidateHttpSession(true)
            }
        val securityFilterChain = http.build()
        log.info(
            "Active filters: {}",
            securityFilterChain.filters.joinToString(separator = "\n- ", prefix = "\n- ") { it.javaClass.name })
        return securityFilterChain
    }
}

@Component
class OAuth2LoginSuccessHandler(
    private val jwtEncoder: org.springframework.security.oauth2.jwt.JwtEncoder,
    @Value("\${security.jwt.ttl-seconds:86400}")
    private val ttlSeconds: Long
) : AuthenticationSuccessHandler {

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauth2User = authentication.principal as OAuth2User
        val now = java.time.Instant.now()
        val claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
            .subject(oauth2User.attributes["sub"] as String)
            .claim("name", oauth2User.name)
            .claim("sessionId", request.session.id)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(ttlSeconds))
            .build()

        val jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue
        val cookie = Cookie("AUTH_TOKEN", token)
        cookie.isHttpOnly = true
        cookie.secure = request.isSecure
        cookie.path = "/"
        cookie.maxAge = ttlSeconds.toInt()

        response.addCookie(cookie)
        response.sendRedirect("/kanban")
    }
}

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
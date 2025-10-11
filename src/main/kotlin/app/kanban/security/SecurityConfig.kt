package app.kanban.security

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.DeferredSecurityContext
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Component

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val trimDownSecurityContextRepository: TrimDownSecurityContextRepository
) {
    private val log = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.ignoringRequestMatchers("/kanban/**")/*TODO make csrf works*/ }
            .authorizeHttpRequests {
                it.requestMatchers("/login", "/oauth2/**", "/css/**", "/js/**").permitAll()
                    .anyRequest().authenticated()
            }.oauth2Login { it.defaultSuccessUrl("/kanban", true) }
            .securityContext { it.securityContextRepository(trimDownSecurityContextRepository) }
            .logout { it.clearAuthentication(true).invalidateHttpSession(true) }
        val securityFilterChain = http.build()
        log.info("Active filters: {}", securityFilterChain.filters.joinToString("\n- ") { it.javaClass.name })
        return securityFilterChain
    }
}

@Component
class TrimDownSecurityContextRepository(
    private val jwtEncoder: JwtEncoder,
    private val jwtDecoder: JwtDecoder
) : HttpSessionSecurityContextRepository() {
    private val log = LoggerFactory.getLogger(TrimDownSecurityContextRepository::class.java)

    override fun saveContext(
        context: SecurityContext,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        if (context.authentication == null) {
            return
        }

        val oAuth2User = context.authentication.principal as OAuth2User
        val name = oAuth2User.getAttribute<String>("name") ?: ""
        val email = oAuth2User.getAttribute<String>("email") ?: ""
        val sessionId = request.session.id
        val authSource = "oauth2"

        val now = java.time.Instant.now()
        val jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build()
        val claims = JwtClaimsSet.builder()
            .claim("name", name)
            .claim("email", email)
            .claim("sessionId", sessionId)
            .claim("authSource", authSource)
            .expiresAt(now.plusSeconds(3600))
            .build()
        val encodedJwt = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue

        val cookie = Cookie("JWT_INFO", encodedJwt)
        cookie.isHttpOnly = true
        cookie.secure = request.isSecure
        cookie.path = "/"
        cookie.maxAge = 3600
        response.addCookie(cookie)
    }

    override fun loadDeferredContext(request: HttpServletRequest): DeferredSecurityContext {
        val cookie = request.cookies?.find { it.name == "JWT_INFO" }
        if (cookie == null) {
            return super.loadDeferredContext(request)
        }
        val jwt = jwtDecoder.decode(cookie.value)
        val user = KanbanUser(jwt.claims["name"]?.toString(), jwt.claims["email"]?.toString(), null)
        return object : DeferredSecurityContext{
            override fun isGenerated(): Boolean {
                return true
            }
            override fun get(): SecurityContext {
                val authentication = object : AbstractAuthenticationToken(listOf()) {
                    init {
                        isAuthenticated = true
                    }

                    override fun getCredentials() = null
                    override fun getPrincipal() = user
                }

                return object : SecurityContext {
                    override fun getAuthentication() = authentication
                    override fun setAuthentication(authentication: Authentication) {
                        throw UnsupportedOperationException("Cannot set authentication on TrimDownSecurityContextRepository")
                    }
                }
            }
        }
    }
}

data class KanbanUser(
    val name: String?,
    val email: String?,
    val phoneNumber: String?
)
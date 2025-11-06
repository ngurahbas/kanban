package app.kanban.security

import app.kanban.user.IdentifierRepository
import app.kanban.user.IdentifierType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Component

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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

        return http.build()
    }
}

@Component
class TrimDownSecurityContextRepository(
    private val identifierRepository: IdentifierRepository
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
        val email = oAuth2User.getAttribute<String>("email") ?: ""

        val identifierId = identifierRepository.insertOrGet(IdentifierType.EMAIL, email)
        log.info("Saved identifier id: $identifierId")

        val initialAuthenticated = context.authentication.isAuthenticated

        context.authentication = object : Authentication {
            private var authenticated = initialAuthenticated

            override fun getAuthorities() = listOf<GrantedAuthority>()

            override fun getCredentials() = null

            override fun getDetails() = null

            override fun getPrincipal() = KanbanUser(identifierId, email, null)

            override fun isAuthenticated(): Boolean {
                return authenticated;
            }

            override fun setAuthenticated(authenticated: Boolean) {
                this.authenticated = authenticated
            }

            override fun getName() = ""
        }

        super.saveContext(context, request, response)
    }
}

data class KanbanUser(
    val identifierId: Long,
    val email: String?,
    val phoneNumber: String?
)

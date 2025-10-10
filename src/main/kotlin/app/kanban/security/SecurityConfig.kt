package app.kanban.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
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
class TrimDownSecurityContextRepository : HttpSessionSecurityContextRepository() {
    override fun saveContext(
        context: SecurityContext,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        context.authentication = object : Authentication {
            private var authenticated: Boolean = context.authentication.isAuthenticated

            override fun getAuthorities() = listOf<GrantedAuthority>()

            override fun getCredentials() = null

            override fun getDetails() = null

            override fun getPrincipal() = null

            override fun isAuthenticated() = authenticated

            override fun setAuthenticated(authenticated: Boolean) {
                this.authenticated = authenticated
            }

            override fun getName() = "left empty"
        }

        super.saveContext(context, request, response)
    }
}
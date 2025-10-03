package app.kanban.security

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

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
            }
            .addFilterBefore(jwtAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter::class.java)
            .oauth2Login { oauth2Login ->
                oauth2Login.successHandler(oAuth2LoginSuccessHandler)
            }.logout {
                it.clearAuthentication(true).invalidateHttpSession(true)
            }
        val securityFilterChain = http.build()
        log.info("Active filters: {}", securityFilterChain.filters.joinToString(separator = "\n- ", prefix = "\n- ") { it.javaClass.name })
        return securityFilterChain
    }
}

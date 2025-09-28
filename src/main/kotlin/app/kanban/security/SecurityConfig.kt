package app.kanban.security

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
            }
            .logout { logout ->
                logout.logoutUrl("/logout")
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID", "AUTH_TOKEN")
                    .logoutSuccessUrl("/login")
            }
        return http.build()
    }
}

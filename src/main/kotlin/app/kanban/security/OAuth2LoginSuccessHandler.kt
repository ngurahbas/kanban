package app.kanban.security

import jakarta.servlet.ServletException
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.io.IOException
import java.time.Duration

@Component
class OAuth2LoginSuccessHandler(
    private val jwtService: JwtService
) : AuthenticationSuccessHandler {

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val principal = authentication.principal
        val (subject, name) = when (principal) {
            is OAuth2User -> {
                val sub = principal.getAttribute<String>("sub") ?: principal.name
                val nm = principal.getAttribute<String>("name") ?: principal.getAttribute("email") ?: principal.name
                sub to nm
            }
            else -> authentication.name to authentication.name
        }
        val authorities = authentication.authorities.map(GrantedAuthority::getAuthority)
        val token = jwtService.generate(subject, name, authorities)
        val cookie = Cookie("AUTH_TOKEN", token)
        cookie.isHttpOnly = true
        cookie.secure = request.isSecure
        cookie.path = "/"
        cookie.maxAge = Duration.ofDays(1).seconds.toInt()
        response.addCookie(cookie)
        val header = buildString {
            append("AUTH_TOKEN=").append(token)
            append("; Path=/; Max-Age=").append(cookie.maxAge)
            append(if (cookie.secure) "; Secure" else "")
            append("; HttpOnly; SameSite=Lax")
        }
        response.addHeader("Set-Cookie", header)
        response.sendRedirect("/kanban")
    }
}

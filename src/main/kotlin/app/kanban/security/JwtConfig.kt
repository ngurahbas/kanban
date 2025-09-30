package app.kanban.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import javax.crypto.spec.SecretKeySpec
import com.nimbusds.jose.jwk.source.ImmutableSecret

@Configuration
class JwtConfig(
    @Value("\${security.jwt.secret:changeit-changeit-changeit-changeit-changeit}")
    private val secret: String
) {

    private fun secretKey() = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")

    @Bean
    fun jwtEncoder(): JwtEncoder {
        return NimbusJwtEncoder(ImmutableSecret(secretKey()))
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val decoder = NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(MacAlgorithm.HS256).build()
        return decoder
    }
}

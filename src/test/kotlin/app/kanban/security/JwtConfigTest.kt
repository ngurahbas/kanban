package app.kanban.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader

@SpringBootTest
class JwtConfigTest(@Autowired val jwtEncoder: JwtEncoder) {

    @Test
    fun `encoder can encode`() {
        val now = java.time.Instant.now()
        val claims = JwtClaimsSet.builder()
            .subject("subject")
            .claim("name", "name")
            .claim("authorities", "authorities")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(1))
            .build()
        val jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build()
        val encoded = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims))
        assertNotNull(encoded)
    }
}
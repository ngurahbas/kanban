package app.kanban.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Minimal JWT (HS256) utility without external libraries.
 * Only supports compact JWS with header {"alg":"HS256","typ":"JWT"}.
 */
@Component
class JwtService(
    @Value("\${security.jwt.secret:changeit-changeit-changeit}")
    private val secret: String,
    @Value("\${security.jwt.ttl-seconds:86400}") // default 1 day
    private val ttlSeconds: Long
) {
    private val base64Url = Base64.getUrlEncoder().withoutPadding()
    private val base64UrlDecoder = Base64.getUrlDecoder()
    private val hmacKey by lazy { SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256") }

    fun generate(subject: String, name: String?, authorities: Collection<String> = emptyList()): String {
        val headerJson = "{" + "\"alg\":\"HS256\",\"typ\":\"JWT\"" + "}"
        val now = Instant.now().epochSecond
        val exp = now + ttlSeconds
        val authArray = authorities.joinToString(prefix = "[", postfix = "]") { "\"" + it.replace("\"", "") + "\"" }
        val claimsJson = buildString {
            append('{')
            append("\"sub\":\"").append(jsonEscape(subject)).append('\"')
            if (!name.isNullOrBlank()) {
                append(',').append("\"name\":\"").append(jsonEscape(name)).append('\"')
            }
            append(',').append("\"exp\":").append(exp)
            append(',').append("\"iat\":").append(now)
            append(',').append("\"authorities\":").append(authArray)
            append('}')
        }
        val header = base64Url.encodeToString(headerJson.toByteArray(StandardCharsets.UTF_8))
        val payload = base64Url.encodeToString(claimsJson.toByteArray(StandardCharsets.UTF_8))
        val signature = sign("$header.$payload")
        return "$header.$payload.$signature"
    }

    data class JwtPrincipal(
        val subject: String,
        val name: String?,
        val authorities: List<String>,
        val exp: Long,
        val iat: Long
    )

    fun parseAndValidate(token: String): JwtPrincipal? {
        val parts = token.split('.')
        if (parts.size != 3) return null
        val (header, payload, signature) = parts
        val expectedSig = sign("$header.$payload")
        if (!constantTimeEquals(signature, expectedSig)) return null
        val payloadJson = String(base64UrlDecoder.decode(payload), StandardCharsets.UTF_8)
        val map = parseJsonObject(payloadJson) ?: return null
        val exp = (map["exp"] as? Number)?.toLong() ?: return null
        if (Instant.now().epochSecond >= exp) return null
        val sub = map["sub"] as? String ?: return null
        val name = map["name"] as? String
        val auth = (map["authorities"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val iat = (map["iat"] as? Number)?.toLong() ?: 0L
        return JwtPrincipal(sub, name, auth, exp, iat)
    }

    private fun sign(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(hmacKey)
        val sig = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return base64Url.encodeToString(sig)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(StandardCharsets.UTF_8)
        val bBytes = b.toByteArray(StandardCharsets.UTF_8)
        val max = maxOf(aBytes.size, bBytes.size)
        var result = 0
        for (i in 0 until max) {
            val x = if (i < aBytes.size) aBytes[i] else 0
            val y = if (i < bBytes.size) bBytes[i] else 0
            result = result or (x.toInt() xor y.toInt())
        }
        return result == 0
    }

    private fun jsonEscape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    // Minimal JSON parser for our predictable payload structure.
    private fun parseJsonObject(json: String): Map<String, Any?>? {
        // Very small, naive parser: only handles flat objects with string/number/array-of-strings values
        try {
            var i = json.indexOf('{') + 1
            val end = json.lastIndexOf('}')
            val map = mutableMapOf<String, Any?>()
            while (i in 1 until end) {
                while (i < end && json[i].isWhitespace()) i++
                if (json[i] == ',') { i++; continue }
                if (json[i] != '"') return null
                val keyStart = ++i
                val keyEnd = json.indexOf('"', keyStart)
                if (keyEnd <= keyStart) return null
                val key = json.substring(keyStart, keyEnd)
                i = keyEnd + 1
                while (i < end && (json[i].isWhitespace() || json[i] == ':')) i++
                when (json[i]) {
                    '"' -> {
                        val valStart = ++i
                        val valEnd = json.indexOf('"', valStart)
                        if (valEnd < 0) return null
                        val value = json.substring(valStart, valEnd)
                        map[key] = value
                        i = valEnd + 1
                    }
                    '[' -> {
                        i++
                        val list = mutableListOf<String>()
                        while (i < end && json[i] != ']') {
                            while (i < end && (json[i].isWhitespace() || json[i] == ',')) i++
                            if (json[i] == '"') {
                                val vs = ++i
                                val ve = json.indexOf('"', vs)
                                list.add(json.substring(vs, ve))
                                i = ve + 1
                            }
                        }
                        i++
                        map[key] = list
                    }
                    else -> {
                        val valStart = i
                        while (i < end && json[i] !in charArrayOf(',', '}')) i++
                        val raw = json.substring(valStart, i).trim()
                        val num = raw.toLongOrNull() ?: raw.toDoubleOrNull()
                        map[key] = num
                    }
                }
            }
            return map
        } catch (_: Exception) {
            return null
        }
    }
}

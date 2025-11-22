package app.kanban.web

import java.math.BigInteger

private val base62chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

fun toBase62(input: String): String {
    val textBytes = input.toByteArray(Charsets.UTF_8)
    var num = BigInteger(1, textBytes)

    if (num == BigInteger.ZERO) {
        return base62chars[0].toString()
    }

    val result = StringBuilder()
    val base = BigInteger.valueOf(62)

    while (num > BigInteger.ZERO) {
        val remainder = num.mod(base).toInt()
        result.append(base62chars[remainder])
        num = num.divide(base)
    }

    return result.reverse().toString()
}
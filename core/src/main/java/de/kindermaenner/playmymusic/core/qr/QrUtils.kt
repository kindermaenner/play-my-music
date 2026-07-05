package de.kindermaenner.playmymusic.core.qr

private val qrRegex = Regex("^[A-Z0-9]{2,5}-[0-9]{1,3}$")

fun isValidQr(text: String): Boolean {
    return qrRegex.matches(text)
}

fun parseQr(text: String): Pair<String, String>? {
    if (!isValidQr(text)) return null
    val parts = text.split("-", limit = 2)
    return parts[0] to parts[1].padStart(3, '0')
}

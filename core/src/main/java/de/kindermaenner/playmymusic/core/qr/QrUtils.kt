package de.kindermaenner.playmymusic.core.qr

data class ParsedQrCode(
    val urlPart: String,
    val trackId: Int
)

fun isValidQr(text: String, validationPrefix: String): Boolean {
    return parseQr(text, validationPrefix) != null
}

fun parseQr(text: String, validationPrefix: String): ParsedQrCode? {
    val trimmedText = text.trim()
    if (trimmedText.isEmpty()) {
        return null
    }

    val suffix = extractSuffixAfterValidation(trimmedText, validationPrefix.trim()) ?: return null
    val segments = suffix.split('/').filter { it.isNotBlank() }

    if (segments.isEmpty()) {
        return null
    }

    val trackSegment = when (segments.size) {
        1 -> segments[0]
        2 -> segments[1]
        else -> return null
    }
    val urlPart = if (segments.size == 2) segments[0] else ""
    val trackId = trackSegment.toIntOrNull() ?: return null

    return ParsedQrCode(urlPart = urlPart, trackId = trackId)
}

private fun extractSuffixAfterValidation(rawValue: String, validationPrefix: String): String? {
    val normalizedRaw = normalizeForValidation(rawValue)

    return if (validationPrefix.isBlank()) {
        val segments = normalizedRaw.split('/').filter { it.isNotBlank() }
        val idSegment = segments.lastOrNull() ?: return null
        if (idSegment.toIntOrNull() == null) {
            return null
        }

        val maybeUrlPart = segments.dropLast(1).lastOrNull().orEmpty()
        val hasLocaleSegment = maybeUrlPart.length == 2 && maybeUrlPart.all { it.isLetter() }
        val urlPart = if (maybeUrlPart.isNotBlank() && !hasLocaleSegment) maybeUrlPart else ""

        when {
            urlPart.isNotBlank() -> "$urlPart/$idSegment"
            else -> idSegment
        }
    } else {
        val normalizedPrefix = normalizeForValidation(validationPrefix)
        val index = normalizedRaw.indexOf(normalizedPrefix, ignoreCase = true)
        if (index < 0) {
            return null
        }

        normalizedRaw.substring(index + normalizedPrefix.length).trim('/')
    }
}

private fun normalizeForValidation(input: String): String {
    return input
        .trim()
        .substringBefore('?')
        .substringBefore('#')
        .removePrefix("http://")
        .removePrefix("https://")
        .trim('/')
}

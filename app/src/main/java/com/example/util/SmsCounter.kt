package com.example.util

data class SmsCount(
    val characters: Int,
    val parts: Int,
    /** True when the text needs Unicode (e.g. ɛ, ɔ, emoji), which shrinks each SMS to 70 characters. */
    val isUnicode: Boolean,
    val charactersPerPart: Int
)

/**
 * Counts SMS parts the way networks bill them.
 *
 * Plain text (GSM-7) fits 160 characters in one SMS, or 153 per part when split. Any character
 * outside GSM-7 switches the whole message to Unicode: 70 per SMS, 67 per part.
 */
object SmsCounter {
    private const val GSM_BASIC =
        "@£\$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?" +
            "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà"
    private const val GSM_EXTENDED = "^{}\\[~]|€\u000C"

    private val gsmBasicSet = GSM_BASIC.toSet()
    private val gsmExtendedSet = GSM_EXTENDED.toSet()

    private val smartPunctuation = mapOf(
        '‘' to "'", '’' to "'", '‚' to "'", '‛' to "'",
        '“' to "\"", '”' to "\"", '„' to "\"",
        '–' to "-", '—' to "-", '…' to "...",
        ' ' to " ", '•' to "-"
    )

    /**
     * Replaces phone-keyboard "smart" punctuation (curly quotes, long dashes) with plain
     * equivalents. One curly apostrophe would otherwise double the cost of a message.
     */
    fun simplifyPunctuation(text: String): String {
        if (text.none { it in smartPunctuation }) return text
        return buildString(text.length) {
            for (ch in text) append(smartPunctuation[ch] ?: ch)
        }
    }

    fun count(text: String): SmsCount {
        if (text.isEmpty()) return SmsCount(0, 0, false, 160)
        val unicode = text.any { it !in gsmBasicSet && it !in gsmExtendedSet }
        return if (unicode) {
            val units = text.length
            val parts = if (units <= 70) 1 else (units + 66) / 67
            SmsCount(units, parts, true, if (parts == 1) 70 else 67)
        } else {
            var septets = 0
            for (ch in text) septets += if (ch in gsmExtendedSet) 2 else 1
            val parts = if (septets <= 160) 1 else (septets + 152) / 153
            SmsCount(septets, parts, false, if (parts == 1) 160 else 153)
        }
    }

    /** Characters that force Unicode, so the UI can tell users exactly what to change. */
    fun unicodeCharacters(text: String): List<Char> =
        text.filter { it !in gsmBasicSet && it !in gsmExtendedSet && !it.isSurrogate() }
            .toList()
            .distinct()
}

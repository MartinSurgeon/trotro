package com.example.util

/**
 * Phone number helpers tuned for Ghana.
 *
 * Numbers are stored in local form (`0244102030`) when they are Ghanaian, or with a leading `+`
 * for other countries. This keeps the list readable for users while letting the SMS gateway
 * receive the international form it expects.
 */
object PhoneNumbers {

    private val networkPrefixes = mapOf(
        "024" to "MTN", "025" to "MTN", "053" to "MTN", "054" to "MTN", "055" to "MTN", "059" to "MTN",
        "020" to "Telecel", "050" to "Telecel",
        "026" to "AirtelTigo", "027" to "AirtelTigo", "056" to "AirtelTigo", "057" to "AirtelTigo"
    )

    /** Keeps digits and a single leading '+'. */
    fun sanitize(raw: String): String {
        val trimmed = raw.trim()
        val digits = trimmed.filter { it.isDigit() }
        return if (trimmed.startsWith("+")) "+$digits" else digits
    }

    /**
     * Returns the number in storage form, or null when it cannot be a real phone number.
     *
     * Handles the common ways Ghanaian numbers arrive: `0244102030`, `+233244102030`,
     * `233244102030`, `+233 0244102030`, `00233...`, and `244102030` (Excel drops the leading 0).
     */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        var digits = trimmed.filter { it.isDigit() }
        var international = trimmed.startsWith("+")
        if (digits.startsWith("00")) {
            digits = digits.removePrefix("00")
            international = true
        }
        return when {
            digits.length == 12 && digits.startsWith("233") -> "0" + digits.substring(3)
            digits.length == 13 && digits.startsWith("2330") -> "0" + digits.substring(4)
            digits.length == 10 && digits.startsWith("0") && !international -> digits
            digits.length == 9 && !digits.startsWith("0") && !international -> "0$digits"
            international && digits.length in 8..15 -> "+$digits"
            else -> null
        }
    }

    fun isValid(raw: String): Boolean = normalize(raw) != null

    /** Form expected by SMSOnlineGH: country code, no '+', e.g. `233244102030`. */
    fun toInternational(stored: String): String {
        val normalized = normalize(stored) ?: return stored.filter { it.isDigit() }
        return if (normalized.startsWith("0")) "233" + normalized.substring(1) else normalized.removePrefix("+")
    }

    /** `0244102030` -> `024 410 2030`; anything else is returned as-is. */
    fun format(stored: String): String {
        val normalized = normalize(stored) ?: return stored
        return if (normalized.length == 10 && normalized.startsWith("0")) {
            "${normalized.substring(0, 3)} ${normalized.substring(3, 6)} ${normalized.substring(6)}"
        } else {
            normalized
        }
    }

    /** Mobile network name for Ghanaian numbers, or null if unknown. */
    fun network(stored: String): String? {
        val normalized = normalize(stored) ?: return null
        if (!normalized.startsWith("0")) return null
        return networkPrefixes[normalized.take(3)]
    }

    /** Key used to detect duplicates regardless of how the number was typed. */
    fun dedupeKey(stored: String): String = (normalize(stored) ?: stored).filter { it.isDigit() }.takeLast(9)
}

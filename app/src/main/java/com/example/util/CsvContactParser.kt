package com.example.util

import com.example.data.ContactEntity

data class CsvParseResult(
    val contacts: List<ContactEntity>,
    val skippedRows: Int
)

object CsvContactParser {
    /** Excel adds this invisible marker to the start of UTF-8 CSV files. */
    private val BYTE_ORDER_MARK = 0xFEFF.toChar().toString()

    /**
     * Parses CSV content into ContactEntity objects.
     * Supports:
     * - Two or more columns: Name, Phone Number, (optional) Group
     * - Reversed columns if first column looks like a phone number and second is a name
     * - Single column of phone numbers
     * - Comma, semicolon, or tab delimiters
     * - Header row detection (e.g., "name,phone", "Full Name,Mobile")
     * - Excel's UTF-8 byte order mark and numbers that lost their leading zero
     * Rows repeating a number already seen earlier in the file are dropped.
     */
    fun parseCsv(rawText: String): CsvParseResult {
        val lines = rawText
            .removePrefix(BYTE_ORDER_MARK)
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        if (lines.isEmpty()) {
            return CsvParseResult(emptyList(), 0)
        }

        val parsedContacts = mutableListOf<ContactEntity>()
        val seenKeys = mutableSetOf<String>()
        var skipped = 0

        fun addContact(name: String, phone: String, group: String) {
            if (seenKeys.add(PhoneNumbers.dedupeKey(phone))) {
                parsedContacts.add(ContactEntity(name = name, phoneNumber = phone, groupTag = group, isSelected = true))
            }
        }

        lines.forEachIndexed { index, line ->
            val columns = splitCsvLine(line)
            if (columns.all { it.isBlank() }) {
                skipped++
                return@forEachIndexed
            }

            // Detect header on first line
            if (index == 0 && isLikelyHeader(columns)) {
                return@forEachIndexed
            }

            if (columns.size == 1) {
                val phone = PhoneNumbers.normalize(columns[0])
                if (phone != null) addContact("", phone, "") else skipped++
                return@forEachIndexed
            }

            val col0 = columns[0].trim()
            val col1 = columns[1].trim()
            val group = columns.getOrNull(2)?.trim().orEmpty()

            val phoneInCol1 = PhoneNumbers.normalize(col1)
            val phoneInCol0 = PhoneNumbers.normalize(col0)
            when {
                phoneInCol1 != null -> addContact(col0, phoneInCol1, group)
                phoneInCol0 != null -> addContact(col1, phoneInCol0, group)
                else -> skipped++
            }
        }

        return CsvParseResult(parsedContacts, skipped)
    }

    private fun isLikelyHeader(columns: List<String>): Boolean {
        val joined = columns.joinToString(" ").lowercase()
        val hasHeaderWord = joined.contains("name") ||
            joined.contains("phone") ||
            joined.contains("mobile") ||
            joined.contains("number") ||
            joined.contains("contact") ||
            joined.contains("tel")
        val hasDigits = joined.count { it.isDigit() } >= 6
        return hasHeaderWord && !hasDigits
    }

    private fun splitCsvLine(line: String): List<String> {
        val delimiter = when {
            line.contains(',') -> ','
            line.contains(';') -> ';'
            line.contains('\t') -> '\t'
            else -> ','
        }
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == delimiter && !inQuotes -> {
                    result.add(current.toString().trim().removeSurrounding("\""))
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim().removeSurrounding("\""))
        return result
    }
}

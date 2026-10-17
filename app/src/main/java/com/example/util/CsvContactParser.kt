package com.example.util

import com.example.data.ContactEntity

data class CsvParseResult(
    val contacts: List<ContactEntity>,
    val skippedRows: Int
)

object CsvContactParser {

    /**
     * Parses CSV content into ContactEntity objects.
     * Supports:
     * - Two or more columns: Name, Phone Number, (optional) Group
     * - Reversed columns if first column looks like a phone number and second is a name
     * - Single column of phone numbers
     * - Comma, semicolon, or tab delimiters
     * - Header row detection (e.g., "name,phone", "Full Name,Mobile")
     */
    fun parseCsv(rawText: String): CsvParseResult {
        val lines = rawText
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        if (lines.isEmpty()) {
            return CsvParseResult(emptyList(), 0)
        }

        val parsedContacts = mutableListOf<ContactEntity>()
        var skipped = 0

        lines.forEachIndexed { index, line ->
            val columns = splitCsvLine(line)
            if (columns.isEmpty()) {
                skipped++
                return@forEachIndexed
            }

            // Detect header on first line
            if (index == 0 && isLikelyHeader(columns)) {
                return@forEachIndexed
            }

            when {
                columns.size == 1 -> {
                    val rawPhone = sanitizePhone(columns[0])
                    if (isValidPhone(rawPhone)) {
                        parsedContacts.add(
                            ContactEntity(
                                name = "Contact $rawPhone",
                                phoneNumber = rawPhone,
                                groupTag = "Imported",
                                isSelected = true
                            )
                        )
                    } else {
                        skipped++
                    }
                }
                else -> {
                    val col0 = columns[0].trim()
                    val col1 = columns[1].trim()
                    val col2 = columns.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() } ?: "Imported"

                    val col0Phone = sanitizePhone(col0)
                    val col1Phone = sanitizePhone(col1)

                    if (isValidPhone(col1Phone)) {
                        val name = col0.ifEmpty { "Contact $col1Phone" }
                        parsedContacts.add(
                            ContactEntity(
                                name = name,
                                phoneNumber = col1Phone,
                                groupTag = col2,
                                isSelected = true
                            )
                        )
                    } else if (isValidPhone(col0Phone)) {
                        val name = col1.ifEmpty { "Contact $col0Phone" }
                        parsedContacts.add(
                            ContactEntity(
                                name = name,
                                phoneNumber = col0Phone,
                                groupTag = col2,
                                isSelected = true
                            )
                        )
                    } else {
                        skipped++
                    }
                }
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

    fun sanitizePhone(raw: String): String {
        val trimmed = raw.trim()
        val sb = StringBuilder()
        for (i in trimmed.indices) {
            val c = trimmed[i]
            if (c == '+' && sb.isEmpty()) {
                sb.append(c)
            } else if (c.isDigit()) {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    fun isValidPhone(sanitized: String): Boolean {
        val digitsCount = sanitized.count { it.isDigit() }
        return digitsCount in 6..16
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

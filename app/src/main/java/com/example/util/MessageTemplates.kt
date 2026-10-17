package com.example.util

data class MessageTemplate(val title: String, val text: String)

object MessageTemplates {
    const val NAME_TOKEN = "{name}"

    private val nameTokenRegex = Regex("""\{\$?name\}""", RegexOption.IGNORE_CASE)
    private val blankRegex = Regex("""\[[^\[\]\n]{1,40}\]""")

    // "GHS" rather than the cedi sign: the cedi sign is not in the SMS alphabet and would
    // turn the whole message into Unicode, halving how much fits in one SMS.
    val all = listOf(
        MessageTemplate(
            "Meeting reminder",
            "Hello {name}, this is a reminder of our meeting on [day] at [time] at [place]. Please be on time. Thank you."
        ),
        MessageTemplate(
            "Church announcement",
            "Dear {name}, you are warmly invited to our church service this Sunday at [time]. Come with a friend. God bless you."
        ),
        MessageTemplate(
            "Dues reminder",
            "Hello {name}, kindly remember to pay your dues of GHS [amount] by [date]. MoMo number: [number]. Thank you."
        ),
        MessageTemplate(
            "Funeral announcement",
            "Dear {name}, with deep sorrow we announce the passing of [name]. The funeral will be held on [date] at [place]. Your presence will be appreciated."
        ),
        MessageTemplate(
            "Invitation",
            "Hello {name}, you are cordially invited to [event] on [date] at [place]. We hope to see you there!"
        ),
        MessageTemplate(
            "Shop promotion",
            "Hello {name}! [product] is now available at [shop name]. Call or WhatsApp [number] to order. Thank you for your support."
        ),
        MessageTemplate(
            "Vote appeal",
            "Dear {name}, I humbly ask for your vote and support in the upcoming election. Together, we can work for change. Thank you."
        ),
        MessageTemplate(
            "Festive greetings",
            "Dear {name}, Merry Christmas and a Happy New Year! Afehyia pa! Thank you for being with us this year."
        ),
        MessageTemplate(
            "Thank you",
            "Dear {name}, thank you for your support. We truly appreciate you. God bless you."
        )
    )

    fun containsName(text: String): Boolean = nameTokenRegex.containsMatchIn(text)

    /** Template blanks like "[date]" that the user forgot to fill in. */
    fun unfilledBlanks(text: String): List<String> = blankRegex.findAll(text).map { it.value }.toList()

    /**
     * Fills in {name}. When there is no name to insert, the token is removed and the
     * surrounding punctuation tidied, so "Hello {name}, ..." becomes "Hello, ...".
     */
    fun personalize(template: String, name: String?): String {
        val cleanName = name?.trim().orEmpty()
        val replaced = template.replace(nameTokenRegex, Regex.escapeReplacement(cleanName))
        if (cleanName.isNotEmpty()) return replaced
        return replaced
            .replace(Regex(""" +([,.!?;:])"""), "$1")
            .replace(Regex(""" {2,}"""), " ")
            .trim()
    }
}

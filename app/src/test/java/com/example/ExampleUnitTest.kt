package com.example

import com.example.util.CsvContactParser
import com.example.util.MessageTemplates
import com.example.util.PhoneNumbers
import com.example.util.SmsCounter
import com.example.viewmodel.CampaignViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    private val BOM = 0xFEFF.toChar().toString()

    @Test
    fun csvContactParser_parsesStandardCsvWithHeaders() {
        val csv = """
            Name,Phone Number,Group
            Abena Mensah,+233244102030,Women's Wing
            Kofi Owusu,0208112233,Youth
        """.trimIndent()

        val result = CsvContactParser.parseCsv(csv)
        assertEquals(2, result.contacts.size)
        assertEquals("Abena Mensah", result.contacts[0].name)
        assertEquals("0244102030", result.contacts[0].phoneNumber)
        assertEquals("Women's Wing", result.contacts[0].groupTag)
        assertTrue(result.contacts[0].isSelected)
    }

    @Test
    fun csvContactParser_handlesReversedColumnsAndSingleColumnPhones() {
        val csv = """
            +233541998877,Ama Serwaa
            0277445566
        """.trimIndent()

        val result = CsvContactParser.parseCsv(csv)
        assertEquals(2, result.contacts.size)
        assertEquals("Ama Serwaa", result.contacts[0].name)
        assertEquals("0541998877", result.contacts[0].phoneNumber)
        assertEquals("0277445566", result.contacts[1].phoneNumber)
        assertEquals("", result.contacts[1].name)
    }

    @Test
    fun csvContactParser_stripsBomAndDropsDuplicatesWithinFile() {
        val csv = BOM + "Name,Phone\nAma,0244102030\nAma again,+233 24 410 2030\nYaw,244102031\nBad,12"
        val result = CsvContactParser.parseCsv(csv)
        assertEquals(listOf("0244102030", "0244102031"), result.contacts.map { it.phoneNumber })
        assertEquals(1, result.skippedRows)
    }

    @Test
    fun phoneNumbers_normalizesGhanaianFormats() {
        assertEquals("0244102030", PhoneNumbers.normalize("024 410 2030"))
        assertEquals("0244102030", PhoneNumbers.normalize("+233244102030"))
        assertEquals("0244102030", PhoneNumbers.normalize("233244102030"))
        assertEquals("0244102030", PhoneNumbers.normalize("+233 0244102030"))
        assertEquals("0244102030", PhoneNumbers.normalize("00233244102030"))
        assertEquals("0244102030", PhoneNumbers.normalize("244102030")) // Excel dropped the 0
        assertEquals("+447700900123", PhoneNumbers.normalize("+44 7700 900123"))
        assertNull(PhoneNumbers.normalize("12345"))
        assertNull(PhoneNumbers.normalize(""))
    }

    @Test
    fun phoneNumbers_formatsAndDetectsNetwork() {
        assertEquals("233244102030", PhoneNumbers.toInternational("0244102030"))
        assertEquals("447700900123", PhoneNumbers.toInternational("+447700900123"))
        assertEquals("024 410 2030", PhoneNumbers.format("+233244102030"))
        assertEquals("MTN", PhoneNumbers.network("0551234567"))
        assertEquals("Telecel", PhoneNumbers.network("0201234567"))
        assertEquals("AirtelTigo", PhoneNumbers.network("0271234567"))
        assertNull(PhoneNumbers.network("+447700900123"))
    }

    @Test
    fun smsCounter_countsPlainAndUnicodeParts() {
        assertEquals(1, SmsCounter.count("a".repeat(160)).parts)
        assertEquals(2, SmsCounter.count("a".repeat(161)).parts)
        assertEquals(2, SmsCounter.count("a".repeat(306)).parts)
        assertEquals(3, SmsCounter.count("a".repeat(307)).parts)

        val twi = "Afehyia pa! Yɛda wo ase."
        assertTrue(SmsCounter.count(twi).isUnicode)
        assertEquals(listOf('ɛ'), SmsCounter.unicodeCharacters(twi))
        assertEquals(2, SmsCounter.count("ɛ".repeat(71)).parts)

        // Extended GSM characters take two slots.
        assertEquals(160, SmsCounter.count("€".repeat(80)).characters)
    }

    @Test
    fun smsCounter_flattensSmartPunctuation() {
        val typed = "Don’t forget – it’s “free”"
        val plain = SmsCounter.simplifyPunctuation(typed)
        assertEquals("Don't forget - it's \"free\"", plain)
        assertFalse(SmsCounter.count(plain).isUnicode)
    }

    @Test
    fun messageTemplates_personalizeAndTidyWhenNameMissing() {
        assertEquals("Hello Ama, see you.", MessageTemplates.personalize("Hello {name}, see you.", "Ama"))
        assertEquals("Hello, see you.", MessageTemplates.personalize("Hello {name}, see you.", ""))
        assertEquals("Dear Kofi", MessageTemplates.personalize("Dear {\$NAME}", "Kofi"))
        assertEquals("Pay \$5", MessageTemplates.personalize("Pay \$5", "Kofi"))
        assertEquals(listOf("[date]", "[place]"), MessageTemplates.unfilledBlanks("On [date] at [place]."))
    }

    @Test
    fun templates_stayInPlainSmsAlphabet() {
        MessageTemplates.all.forEach { template ->
            val sample = MessageTemplates.personalize(template.text, "Ama")
            assertFalse("${template.title} forces Unicode", SmsCounter.count(sample).isUnicode)
        }
    }

    @Test
    fun senderNameValidation() {
        assertTrue(CampaignViewModel.isValidSenderName("GRACE CHAPL"))
        assertTrue(CampaignViewModel.isValidSenderName("Shop24"))
        assertFalse(CampaignViewModel.isValidSenderName("TOO LONG NAME"))
        assertFalse(CampaignViewModel.isValidSenderName("Ama's"))
        assertFalse(CampaignViewModel.isValidSenderName("   "))
    }
}

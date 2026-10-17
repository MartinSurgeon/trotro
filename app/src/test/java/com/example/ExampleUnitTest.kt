package com.example

import com.example.util.CsvContactParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
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
        assertEquals("+233244102030", result.contacts[0].phoneNumber)
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
        assertEquals("+233541998877", result.contacts[0].phoneNumber)
        assertEquals("0277445566", result.contacts[1].phoneNumber)
    }
}

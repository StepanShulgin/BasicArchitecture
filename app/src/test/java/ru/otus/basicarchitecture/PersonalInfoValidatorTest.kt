package ru.otus.basicarchitecture

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalInfoValidatorTest {
    private val today = LocalDate.of(2024, 3, 15)

    @Test
    fun `accepts user who turns 18 today`() {
        assertTrue(PersonalInfoValidator.isValid("Ada", "Lovelace", "15.03.2006", today))
    }

    @Test
    fun `rejects user younger than 18`() {
        assertFalse(PersonalInfoValidator.isValid("Ada", "Lovelace", "16.03.2006", today))
    }

    @Test
    fun `rejects blank names`() {
        assertFalse(PersonalInfoValidator.isValid("  ", "Lovelace", "15.03.1990", today))
        assertFalse(PersonalInfoValidator.isValid("Ada", "", "15.03.1990", today))
    }

    @Test
    fun `rejects malformed and impossible birthdays`() {
        assertFalse(PersonalInfoValidator.isValid("Ada", "Lovelace", "2000-01-01", today))
        assertFalse(PersonalInfoValidator.isValid("Ada", "Lovelace", "31.02.2000", today))
    }

    @Test
    fun `rejects future birthday`() {
        assertFalse(PersonalInfoValidator.isValid("Ada", "Lovelace", "16.03.2024", today))
    }
}

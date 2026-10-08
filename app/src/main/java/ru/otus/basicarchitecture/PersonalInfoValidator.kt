package ru.otus.basicarchitecture

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object PersonalInfoValidator {
    private val birthdayFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")

    fun isValid(firstName: String, lastName: String, birthday: String, today: LocalDate = LocalDate.now()): Boolean {
        if (firstName.isBlank() || lastName.isBlank()) return false
        val birthDate = try {
            LocalDate.parse(birthday, birthdayFormatter)
        } catch (_: DateTimeParseException) {
            return false
        }
        return !birthDate.isAfter(today) && Period.between(birthDate, today).years >= 18
    }
}

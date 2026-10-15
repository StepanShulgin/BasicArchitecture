package ru.otus.basicarchitecture

import javax.inject.Inject
import javax.inject.Singleton

data class WizardProfile(
    val firstName: String = "",
    val lastName: String = "",
    val birthday: String = "",
    val country: String = "",
    val city: String = "",
    val address: String = "",
    val interests: Set<String> = emptySet()
)

@Singleton
class WizardCache @Inject constructor() {
    var profile = WizardProfile()
        private set

    fun savePersonalInfo(firstName: String, lastName: String, birthday: String) {
        profile = profile.copy(firstName = firstName, lastName = lastName, birthday = birthday)
    }

    fun saveAddress(country: String, city: String, address: String) {
        profile = profile.copy(country = country, city = city, address = address)
    }

    fun saveInterests(interests: Set<String>) {
        profile = profile.copy(interests = interests.toSet())
    }
}

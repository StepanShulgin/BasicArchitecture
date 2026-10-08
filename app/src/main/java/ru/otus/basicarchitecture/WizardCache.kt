package ru.otus.basicarchitecture

import javax.inject.Inject
import javax.inject.Singleton

data class WizardProfile(
    var firstName: String = "",
    var lastName: String = "",
    var birthday: String = "",
    var country: String = "",
    var city: String = "",
    var address: String = "",
    var interests: Set<String> = emptySet()
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
        profile = profile.copy(interests = interests)
    }
}

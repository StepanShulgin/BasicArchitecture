package ru.otus.basicarchitecture

import javax.inject.Inject
import javax.inject.Singleton

data class WizardProfile(
    var firstName: String = "",
    var lastName: String = "",
    var birthday: String = "",
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

    fun saveAddress(address: String) {
        profile = profile.copy(address = address)
    }

    fun saveInterests(interests: Set<String>) {
        profile = profile.copy(interests = interests)
    }
}

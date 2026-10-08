package ru.otus.basicarchitecture

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PersonalInfoViewModel @Inject constructor(private val cache: WizardCache) : ViewModel() {
    private val _validationError = MutableLiveData<String>()
    val validationError: LiveData<String> = _validationError

    fun submit(firstName: String, lastName: String, birthday: String): Boolean {
        if (!PersonalInfoValidator.isValid(firstName, lastName, birthday)) {
            _validationError.value = "Введите имя, фамилию и дату рождения. Вам должно быть не менее 18 лет."
            return false
        }
        cache.savePersonalInfo(firstName.trim(), lastName.trim(), birthday)
        return true
    }
}

@HiltViewModel
class AddressViewModel @Inject constructor(private val cache: WizardCache) : ViewModel() {
    fun save(country: String, city: String, address: String) = cache.saveAddress(country.trim(), city.trim(), address.trim())
}

@HiltViewModel
class InterestsViewModel @Inject constructor(private val cache: WizardCache) : ViewModel() {
    val interests = listOf("Путешествия", "Музыка", "Кино", "Спорт", "Книги", "Искусство", "Кулинария", "Технологии", "Природа", "Игры", "Танцы", "Животные")

    fun save(selected: Set<String>) = cache.saveInterests(selected)
}

@HiltViewModel
class SummaryViewModel @Inject constructor(private val cache: WizardCache) : ViewModel() {
    val profile: WizardProfile
        get() = cache.profile
}

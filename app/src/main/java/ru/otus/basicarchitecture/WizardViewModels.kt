package ru.otus.basicarchitecture

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonalInfoViewModel @Inject constructor(private val cache: WizardCache) : ViewModel() {
    private val _validationError = MutableLiveData<String?>()
    val validationError: LiveData<String?> = _validationError
    val draft get() = cache.profile

    fun updateDraft(firstName: String, lastName: String, birthday: String): Boolean {
        cache.savePersonalInfo(firstName, lastName, birthday)
        val valid = PersonalInfoValidator.isValid(firstName, lastName, birthday)
        val error = if (!valid && firstName.isNotBlank() && lastName.isNotBlank() && birthday.length == 10)
            "Введите корректную дату рождения. Вам должно быть не менее 18 лет." else null
        if (_validationError.value != error) _validationError.value = error
        return valid
    }

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
class AddressViewModel @Inject constructor(
    private val cache: WizardCache,
    private val repository: AddressSuggestionsDataSource
) : ViewModel() {
    private val _suggestions = MutableStateFlow<AddressSuggestionState>(AddressSuggestionState.Idle)
    val suggestions: StateFlow<AddressSuggestionState> = _suggestions.asStateFlow()
    private var searchJob: Job? = null
    val address get() = cache.profile.address

    fun search(query: String) {
        searchJob?.cancel()
        if (query.trim().length < MIN_QUERY_LENGTH) {
            _suggestions.value = AddressSuggestionState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            _suggestions.value = AddressSuggestionState.Loading
            delay(SEARCH_DEBOUNCE_MS)
            try {
                val result = repository.search(query.trim())
                // A data source may finish despite cancellation; never display an obsolete response.
                ensureActive()
                _suggestions.value = AddressSuggestionState.Success(result)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: MissingDaDataApiKeyException) {
                ensureActive()
                _suggestions.value = AddressSuggestionState.Error(AddressSuggestionError.MISSING_API_KEY)
            } catch (_: Exception) {
                ensureActive()
                _suggestions.value = AddressSuggestionState.Error(AddressSuggestionError.NETWORK)
            }
        }
    }

    fun save(address: String) = cache.saveAddress(address.trim())

    private companion object {
        const val MIN_QUERY_LENGTH = 3
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}

@HiltViewModel
class InterestsViewModel @Inject constructor(private val cache: WizardCache) : ViewModel() {
    val selectedInterests get() = cache.profile.interests
    val interests = listOf("Путешествия", "Музыка", "Кино", "Спорт", "Книги", "Искусство", "Кулинария", "Технологии", "Природа", "Игры", "Танцы", "Животные")

    fun save(selected: Set<String>) = cache.saveInterests(selected)
}

@HiltViewModel
class SummaryViewModel @Inject constructor(private val cache: WizardCache) : ViewModel() {
    val profile: WizardProfile
        get() = cache.profile
}

package ru.otus.basicarchitecture

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddressViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `short query resets state without calling repository`() = runTest(dispatcher) {
        val repository = FakeAddressSuggestionsDataSource()
        val viewModel = AddressViewModel(WizardCache(), repository)

        viewModel.search(" ab ")
        runCurrent()

        assertEquals(AddressSuggestionState.Idle, viewModel.suggestions.value)
        assertEquals(emptyList<String>(), repository.queries)
    }

    @Test
    fun `search returns suggestions for trimmed query`() = runTest(dispatcher) {
        val suggestions = listOf(AddressSuggestion("Moscow", "Moscow, Russia"))
        val repository = FakeAddressSuggestionsDataSource(result = suggestions)
        val viewModel = AddressViewModel(WizardCache(), repository)

        viewModel.search("  Moscow  ")
        runCurrent()

        assertEquals(AddressSuggestionState.Loading, viewModel.suggestions.value)
        advanceUntilIdle()

        assertEquals(listOf("Moscow"), repository.queries)
        assertEquals(AddressSuggestionState.Success(suggestions), viewModel.suggestions.value)
    }

    @Test
    fun `maps repository failure to network error`() = runTest(dispatcher) {
        val repository = FakeAddressSuggestionsDataSource(failure = IOException("offline"))
        val viewModel = AddressViewModel(WizardCache(), repository)

        viewModel.search("Moscow")
        advanceUntilIdle()

        assertEquals(
            AddressSuggestionState.Error(AddressSuggestionError.NETWORK),
            viewModel.suggestions.value
        )
    }

    @Test
    fun `maps missing api key to configuration error`() = runTest(dispatcher) {
        val repository = FakeAddressSuggestionsDataSource(
            failure = IllegalStateException("DaData API key is not configured")
        )
        val viewModel = AddressViewModel(WizardCache(), repository)

        viewModel.search("Moscow")
        advanceUntilIdle()

        assertEquals(
            AddressSuggestionState.Error(AddressSuggestionError.MISSING_API_KEY),
            viewModel.suggestions.value
        )
    }

    @Test
    fun `new search cancels pending search`() = runTest(dispatcher) {
        val repository = FakeAddressSuggestionsDataSource()
        val viewModel = AddressViewModel(WizardCache(), repository)

        viewModel.search("Old query")
        runCurrent()
        viewModel.search("New query")
        advanceUntilIdle()

        assertEquals(listOf("New query"), repository.queries)
        assertEquals(AddressSuggestionState.Success(emptyList()), viewModel.suggestions.value)
    }

    private class FakeAddressSuggestionsDataSource(
        private val result: List<AddressSuggestion> = emptyList(),
        private val failure: Exception? = null
    ) : AddressSuggestionsDataSource {
        val queries = mutableListOf<String>()

        override suspend fun search(query: String): List<AddressSuggestion> {
            queries += query
            failure?.let { throw it }
            return result
        }
    }
}

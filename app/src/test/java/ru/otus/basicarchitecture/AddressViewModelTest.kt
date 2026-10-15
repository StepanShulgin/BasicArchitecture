package ru.otus.basicarchitecture

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
            failure = MissingDaDataApiKeyException()
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

    @Test
    fun `editing query cancels an in flight request`() = runTest(dispatcher) {
        var cancelled = false
        val repository = object : AddressSuggestionsDataSource {
            override suspend fun search(query: String): List<AddressSuggestion> {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            }
        }
        val viewModel = AddressViewModel(WizardCache(), repository)
        viewModel.search("Москва")
        advanceTimeBy(350)
        runCurrent()
        viewModel.search("")
        runCurrent()
        assertEquals(true, cancelled)
        assertEquals(AddressSuggestionState.Idle, viewModel.suggestions.value)
    }

    @Test
    fun `cancelled non cooperative response cannot overwrite cleared state`() = runTest(dispatcher) {
        val response = CompletableDeferred<List<AddressSuggestion>>()
        val repository = object : AddressSuggestionsDataSource {
            override suspend fun search(query: String) = withContext(NonCancellable) { response.await() }
        }
        val viewModel = AddressViewModel(WizardCache(), repository)
        viewModel.search("Москва")
        advanceTimeBy(350)
        runCurrent()
        viewModel.search("")
        response.complete(listOf(AddressSuggestion("Москва", "Россия, Москва")))
        advanceUntilIdle()
        assertEquals(AddressSuggestionState.Idle, viewModel.suggestions.value)
    }

    @Test
    fun `cancelled failures cannot overwrite cleared state`() = runTest(dispatcher) {
        for (failure in listOf(IOException("offline"), MissingDaDataApiKeyException())) {
            val release = CompletableDeferred<Unit>()
            val repository = object : AddressSuggestionsDataSource {
                override suspend fun search(query: String): List<AddressSuggestion> =
                    withContext(NonCancellable) {
                        release.await()
                        throw failure
                    }
            }
            val viewModel = AddressViewModel(WizardCache(), repository)
            viewModel.search("Old query")
            advanceTimeBy(350)
            runCurrent()

            viewModel.search("")
            release.complete(Unit)
            advanceUntilIdle()

            assertEquals(failure.toString(), AddressSuggestionState.Idle, viewModel.suggestions.value)
        }
    }

    @Test
    fun `cancelled failures cannot overwrite newer success`() = runTest(dispatcher) {
        for (failure in listOf(IOException("offline"), MissingDaDataApiKeyException())) {
            val release = CompletableDeferred<Unit>()
            val suggestions = listOf(AddressSuggestion("New address", "New full address"))
            val repository = object : AddressSuggestionsDataSource {
                override suspend fun search(query: String): List<AddressSuggestion> =
                    if (query == "Old query") {
                        withContext(NonCancellable) {
                            release.await()
                            throw failure
                        }
                    } else {
                        suggestions
                    }
            }
            val viewModel = AddressViewModel(WizardCache(), repository)
            viewModel.search("Old query")
            advanceTimeBy(350)
            runCurrent()

            viewModel.search("New query")
            advanceUntilIdle()
            val expected = AddressSuggestionState.Success(suggestions)
            assertEquals(expected, viewModel.suggestions.value)

            release.complete(Unit)
            advanceUntilIdle()

            assertEquals(failure.toString(), expected, viewModel.suggestions.value)
        }
    }

    @Test
    fun `unrelated illegal state is not reported as a missing key`() = runTest(dispatcher) {
        val viewModel = AddressViewModel(
            WizardCache(), FakeAddressSuggestionsDataSource(failure = IllegalStateException("bad response"))
        )
        viewModel.search("Москва")
        advanceUntilIdle()
        assertEquals(AddressSuggestionState.Error(AddressSuggestionError.NETWORK), viewModel.suggestions.value)
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

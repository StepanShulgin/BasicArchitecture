package ru.otus.basicarchitecture

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

data class AddressSuggestionRequest(val query: String, val count: Int = 8)
data class AddressSuggestionResponse(val suggestions: List<AddressSuggestion> = emptyList())
data class AddressSuggestion(val value: String, val unrestricted_value: String)

sealed interface AddressSuggestionState {
    data object Idle : AddressSuggestionState
    data object Loading : AddressSuggestionState
    data class Success(val suggestions: List<AddressSuggestion>) : AddressSuggestionState
    data class Error(val reason: AddressSuggestionError) : AddressSuggestionState
}

enum class AddressSuggestionError { MISSING_API_KEY, NETWORK }

interface DaDataApi {
    @POST("suggest/address")
    suspend fun suggestAddress(@Body request: AddressSuggestionRequest): AddressSuggestionResponse
}

class AddressSuggestionRepository @Inject constructor(private val api: DaDataApi) {
    suspend fun search(query: String): List<AddressSuggestion> {
        check(BuildConfig.DADATA_API_KEY.isNotBlank()) { "DaData API key is not configured" }
        return api.suggestAddress(AddressSuggestionRequest(query)).suggestions
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AddressNetworkModule {
    @Provides
    @Singleton
    fun provideHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Token ${BuildConfig.DADATA_API_KEY}")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    @Provides
    @Singleton
    fun provideDaDataApi(client: OkHttpClient): DaDataApi = Retrofit.Builder()
        .baseUrl("https://suggestions.dadata.ru/suggestions/api/4_1/rs/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DaDataApi::class.java)
}

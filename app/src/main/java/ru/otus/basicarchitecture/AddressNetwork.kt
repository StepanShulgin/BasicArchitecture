package ru.otus.basicarchitecture

import com.google.gson.annotations.SerializedName
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class AddressSuggestionRequest(val query: String, val count: Int = 8)
data class AddressSuggestionResponse(val suggestions: List<AddressSuggestion> = emptyList())
data class AddressSuggestion(val value: String, @SerializedName("unrestricted_value") val unrestrictedValue: String)

sealed interface AddressSuggestionState {
    data object Idle : AddressSuggestionState
    data object Loading : AddressSuggestionState
    data class Success(val suggestions: List<AddressSuggestion>) : AddressSuggestionState
    data class Error(val reason: AddressSuggestionError) : AddressSuggestionState
}

enum class AddressSuggestionError { MISSING_API_KEY, NETWORK }

class MissingDaDataApiKeyException : IllegalStateException("DaData API key is not configured")

interface DaDataApi {
    @POST("suggest/address")
    suspend fun suggestAddress(@Body request: AddressSuggestionRequest): AddressSuggestionResponse
}

interface AddressSuggestionsDataSource {
    suspend fun search(query: String): List<AddressSuggestion>
}

class AddressSuggestionRepository @Inject constructor(private val api: DaDataApi) : AddressSuggestionsDataSource {
    override suspend fun search(query: String): List<AddressSuggestion> {
        if (BuildConfig.DADATA_API_KEY.isBlank()) throw MissingDaDataApiKeyException()
        return api.suggestAddress(AddressSuggestionRequest(query)).suggestions
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AddressSuggestionDataSourceModule {
    @Binds
    abstract fun bindDataSource(repository: AddressSuggestionRepository): AddressSuggestionsDataSource
}

@Module
@InstallIn(SingletonComponent::class)
object AddressNetworkModule {
    @Provides
    @Singleton
    fun provideHttpClient(): OkHttpClient = createHttpClient(BuildConfig.DADATA_API_KEY)

    internal fun createHttpClient(apiKey: String): OkHttpClient = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", "Token $apiKey")
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

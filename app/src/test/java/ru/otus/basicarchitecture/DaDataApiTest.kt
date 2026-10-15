package ru.otus.basicarchitecture

import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DaDataApiTest {
    @Test
    fun `request uses DaData contract and decodes unrestricted address`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(
                """{"suggestions":[{"value":"г Москва","unrestricted_value":"Россия, г Москва"}]}"""
            ))
            val response = api(server).suggestAddress(AddressSuggestionRequest("Москва"))
            val request = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS))
            assertEquals("POST", request.method)
            assertEquals("/suggest/address", request.path)
            assertEquals("Token test-key", request.getHeader("Authorization"))
            assertEquals("application/json", request.getHeader("Accept"))
            val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals("Москва", body["query"].asString)
            assertEquals(8, body["count"].asInt)
            assertEquals(listOf(AddressSuggestion("г Москва", "Россия, г Москва")), response.suggestions)
        }
    }

    @Test
    fun `forbidden response is propagated instead of returning empty suggestions`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(403).setBody("{}"))
            val error = assertThrows(HttpException::class.java) {
                runBlocking { api(server).suggestAddress(AddressSuggestionRequest("Москва")) }
            }
            assertEquals(403, error.code())
        }
    }

    private fun api(server: MockWebServer): DaDataApi = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(AddressNetworkModule.createHttpClient("test-key"))
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DaDataApi::class.java)
}

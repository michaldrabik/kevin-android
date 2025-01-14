package eu.kevin.demo.auth

import eu.kevin.demo.auth.entities.ApiAuthState
import eu.kevin.demo.auth.entities.ApiPayment
import eu.kevin.demo.auth.entities.InitiateAuthenticationRequest
import eu.kevin.demo.auth.entities.InitiatePaymentRequest
import io.ktor.client.*
import io.ktor.client.request.*

class KevinClient(private val httpClient: HttpClient) : KevinApiClient {

    override suspend fun getAuthState(request: InitiateAuthenticationRequest): String {
        return httpClient.post<ApiAuthState>("auth/initiate/") {
            body = request
        }.state
    }

    override suspend fun getCardAuthState(): String {
        return httpClient.post<ApiAuthState>("auth/initiate/card/").state
    }

    override suspend fun initializeBankPayment(
        request: InitiatePaymentRequest
    ): ApiPayment {
        return httpClient.post("payments/bank/") {
            body = request
        }
    }

    override suspend fun initializeLinkedBankPayment(
        accessToken: String,
        request: InitiatePaymentRequest
    ): ApiPayment {
        return httpClient.post("payments/bank/linked/") {
            header("Authorization", "Bearer $accessToken")
            body = request
        }
    }

    override suspend fun initializeCardPayment(
        request: InitiatePaymentRequest
    ): ApiPayment {
        return httpClient.post("payments/card/") {
            body = request
        }
    }

    override suspend fun initializeHybridPayment(
        request: InitiatePaymentRequest
    ): ApiPayment {
        return httpClient.post("payments/hybrid/") {
            body = request
        }
    }
}
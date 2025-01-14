package eu.kevin.demo.auth.entities

import kotlinx.serialization.Serializable

@Serializable
data class InitiatePaymentRequest(
    val amount: String,
    val email: String,
    val iban: String,
    val creditorName: String
)
package eu.kevin.demo.helpers

import eu.kevin.demo.R
import eu.kevin.inapppayments.paymentsession.enums.PaymentType

object PaymentTypeHelper {

    fun getStringRes(paymentType: PaymentType): Int {
        return when (paymentType) {
            PaymentType.BANK -> R.string.window_main_bank_payment
            PaymentType.CARD -> R.string.window_main_card_payment
        }
    }
}
package eu.kevin.demo.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import eu.kevin.common.extensions.setFragmentResultListener
import eu.kevin.core.entities.SessionResult
import eu.kevin.core.enums.KevinCountry
import eu.kevin.demo.auth.entities.ApiPayment
import eu.kevin.demo.countryselection.CountrySelectionContract
import eu.kevin.demo.main.entities.CreditorListItem
import eu.kevin.demo.main.entities.DonationRequest
import eu.kevin.inapppayments.paymentsession.PaymentSessionContract
import eu.kevin.inapppayments.paymentsession.entities.PaymentSessionConfiguration
import eu.kevin.inapppayments.paymentsession.enums.PaymentType
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainFragment : Fragment(), MainViewCallback {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(this)
    }

    private val makePayment = registerForActivityResult(PaymentSessionContract()) { result ->
        when (result) {
            is SessionResult.Success -> {
                viewModel.onPaymentSuccessful()
            }
            is SessionResult.Failure -> {
                viewModel.onPaymentFailure(result.error)
            }
            is SessionResult.Canceled -> {}
        }
    }

    private lateinit var contentView: MainView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        observeChanges()
        listenForCountrySelectedResult()
        return MainView(inflater.context).also {
            it.callback = this
            contentView = it
        }
    }

    private fun observeChanges() {
        lifecycleScope.launchWhenStarted {
            viewModel.viewState.onEach { viewState ->
                contentView.update(viewState)
            }.launchIn(this)

            viewModel.viewAction.onEach { action ->
                when (action) {
                    is MainViewAction.OpenPaymentSession -> {
                        openPaymentSession(action.payment, action.paymentType)
                    }
                    is MainViewAction.ShowFieldValidations -> {
                        contentView.showInputFieldValidations(
                            action.emailValidationResult,
                            action.amountValidationResult,
                            action.termsAccepted
                        )
                    }
                    is MainViewAction.ShowSuccessDialog -> {
                        contentView.showSuccessDialog()
                    }
                    is MainViewAction.ResetFields -> {
                        contentView.resetFields()
                    }
                }
            }.launchIn(this)
        }
    }

    private fun listenForCountrySelectedResult() {
        parentFragmentManager.setFragmentResultListener(CountrySelectionContract, this) {
            viewModel.onCountrySelected(it)
        }
    }

    private fun openPaymentSession(payment: ApiPayment, paymentType: PaymentType) {
        val config = PaymentSessionConfiguration.Builder(payment.id)
            .setPaymentType(paymentType)
            .setPreselectedCountry(KevinCountry.LITHUANIA)
            .setSkipBankSelection(false)
            .build()
        makePayment.launch(config)
    }

    // MainViewCallback

    override fun onDonateClick(
        donationRequest: DonationRequest
    ) {
        viewModel.donate(donationRequest)
    }

    override fun openUrl(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    override fun onCreditorSelected(creditor: CreditorListItem) {
        viewModel.onCreditorSelected(creditor)
    }

    override fun onSelectCountryClick() {
        viewModel.openCountrySelection()
    }

    override fun onAmountChanged(amount: String) {
        viewModel.onAmountChanged(amount)
    }
}
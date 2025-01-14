package eu.kevin.inapppayments.cardpayment

import android.net.Uri
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import eu.kevin.common.architecture.BaseViewModel
import eu.kevin.common.architecture.routing.GlobalRouter
import eu.kevin.common.entities.LoadingState
import eu.kevin.common.entities.isLoading
import eu.kevin.common.extensions.removeWhiteSpaces
import eu.kevin.common.fragment.FragmentResult
import eu.kevin.core.plugin.Kevin
import eu.kevin.inapppayments.BuildConfig
import eu.kevin.inapppayments.cardpayment.CardPaymentIntent.*
import eu.kevin.inapppayments.cardpayment.CardPaymentViewAction.ShowFieldValidations
import eu.kevin.inapppayments.cardpayment.CardPaymentViewAction.SubmitCardForm
import eu.kevin.common.entities.KevinAmount
import eu.kevin.inapppayments.cardpayment.events.CardPaymentEvent
import eu.kevin.inapppayments.cardpayment.events.CardPaymentEvent.*
import eu.kevin.inapppayments.cardpayment.inputvalidation.CardExpiryDateValidator
import eu.kevin.inapppayments.cardpayment.inputvalidation.CardNumberValidator
import eu.kevin.inapppayments.cardpayment.inputvalidation.CardholderNameValidator
import eu.kevin.inapppayments.cardpayment.inputvalidation.CvvValidator
import eu.kevin.inapppayments.cardpaymentredirect.CardPaymentRedirectContract
import eu.kevin.inapppayments.cardpaymentredirect.CardPaymentRedirectFragmentConfiguration
import eu.kevin.inapppayments.networking.KevinPaymentsClient
import eu.kevin.inapppayments.networking.KevinPaymentsClientProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.*

internal class CardPaymentViewModel(
    savedStateHandle: SavedStateHandle,
    private val kevinPaymentsClient: KevinPaymentsClient
) : BaseViewModel<CardPaymentState, CardPaymentIntent>(savedStateHandle) {
    override fun getInitialData() = CardPaymentState()

    private val _viewAction = Channel<CardPaymentViewAction>(Channel.BUFFERED)
    val viewAction = _viewAction.receiveAsFlow()

    private lateinit var configuration: CardPaymentFragmentConfiguration

    override suspend fun handleIntent(intent: CardPaymentIntent) {
        when (intent) {
            is Initialize -> initialize(intent.configuration)
            is HandleBackClicked -> {
                if (!state.value.loadingState.isLoading()) {
                    GlobalRouter.popCurrentFragment()
                }
            }
            is HandlePageStartLoading -> updateState { it.copy(isContinueEnabled = false) }
            is HandlePageFinishedLoading -> updateState { it.copy(isContinueEnabled = true) }
            is HandleOnContinueClicked -> {
                handleOnContinueClick(
                    intent.cardholderName,
                    intent.cardNumber,
                    intent.expiryDate,
                    intent.cvv
                )
            }
            is HandlePaymentResult -> handlePaymentResult(intent.uri)
            is HandleCardPaymentEvent -> handleCardPaymentEvent(intent.event)
            is HandleUserSoftRedirect -> handleUserSoftRedirect(intent.shouldRedirect)
        }
    }

    private suspend fun initialize(configuration: CardPaymentFragmentConfiguration) {
        this.configuration = configuration
        val baseCardPaymentUrl = if (Kevin.isSandbox()) {
            BuildConfig.KEVIN_SANDBOX_CARD_PAYMENT_URL
        } else {
            BuildConfig.KEVIN_CARD_PAYMENT_URL
        }
        updateState {
            it.copy(
                url = baseCardPaymentUrl.format(configuration.paymentId)
            )
        }
        val paymentInfo = kevinPaymentsClient.getCardPaymentInfo(configuration.paymentId)

        val amount = try {
            KevinAmount(
                paymentInfo.amount,
                Currency.getInstance(paymentInfo.currencyCode)
            )
        } catch (ignored: Exception) {
            null
        }
        updateState {
            it.copy(
                amount = amount
            )
        }
    }

    private suspend fun handleOnContinueClick(
        cardholderName: String,
        cardNumber: String,
        expiryDate: String,
        cvv: String
    ) {
        val cardholderNameValidation = CardholderNameValidator.validate(cardholderName)
        val cardNumberValidation = CardNumberValidator.validate(cardNumber.removeWhiteSpaces())
        val expiryDateValidation = CardExpiryDateValidator.validate(expiryDate)
        val cvvValidation = CvvValidator.validate(cvv)
        _viewAction.trySend(
            ShowFieldValidations(
                cardholderNameValidation,
                cardNumberValidation,
                expiryDateValidation,
                cvvValidation
            )
        )
        if (
            cardholderNameValidation.isValid()
            && cardNumberValidation.isValid()
            && expiryDateValidation.isValid()
            && cvvValidation.isValid()
        ) {
            updateState {
                it.copy(
                    isContinueEnabled = false,
                    loadingState = LoadingState.Loading(true)
                )
            }
            _viewAction.trySend(SubmitCardForm(cardholderName, cardNumber, expiryDate, cvv))
        }
    }

    private fun handlePaymentResult(uri: Uri) {
        val status = uri.getQueryParameter("statusGroup")
        if (status == "completed") {
            val result = CardPaymentResult(
                uri.getQueryParameter("paymentId") ?: ""
            )
            GlobalRouter.returnFragmentResult(CardPaymentContract, FragmentResult.Success(result))
        } else {
            GlobalRouter.returnFragmentResult(CardPaymentContract, FragmentResult.Canceled)
        }
    }

    private suspend fun handleCardPaymentEvent(event: CardPaymentEvent) {
        when (event) {
            is SoftRedirect -> {
                val bankName = try {
                    kevinPaymentsClient.getBankFromCardNumber(
                        configuration.paymentId,
                        event.cardNumber
                    ).name
                } catch (error: Exception) {
                    null
                }
                val configuration = CardPaymentRedirectFragmentConfiguration(
                    bankName
                )
                GlobalRouter.pushModalFragment(CardPaymentRedirectContract.getFragment(configuration))
                updateState {
                    it.copy(
                        loadingState = LoadingState.Loading(false)
                    )
                }
            }
            is HardRedirect -> {
                updateState {
                    it.copy(
                        isContinueEnabled = false,
                        showCardDetails = false,
                        loadingState = LoadingState.Loading(false)
                    )
                }
            }
            is SubmittingCardData -> {
                delay(500)
                updateState {
                    it.copy(
                        isContinueEnabled = false,
                        showCardDetails = false,
                        loadingState = LoadingState.Loading(false)
                    )
                }
            }
        }
    }

    private suspend fun handleUserSoftRedirect(shouldRedirect: Boolean) {
        _viewAction.trySend(CardPaymentViewAction.SubmitUserRedirect(shouldRedirect))
        if (shouldRedirect) {
            updateState {
                it.copy(
                    isContinueEnabled = false,
                    showCardDetails = false
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(owner: SavedStateRegistryOwner) : AbstractSavedStateViewModelFactory(owner, null) {
        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            return CardPaymentViewModel(
                handle,
                KevinPaymentsClientProvider.kevinPaymentsClient
            ) as T
        }
    }
}
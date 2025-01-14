package eu.kevin.demo.main

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kevin.common.entities.LoadingState
import eu.kevin.common.entities.isLoading
import eu.kevin.common.extensions.*
import eu.kevin.common.helpers.ErrorHelper
import eu.kevin.common.helpers.SnackbarHelper
import eu.kevin.common.managers.KeyboardManager
import eu.kevin.demo.R
import eu.kevin.demo.countryselection.helpers.CountryHelper
import eu.kevin.demo.databinding.FragmentMainBinding
import eu.kevin.demo.extensions.getCurrentLocale
import eu.kevin.demo.extensions.removeNumberSeparator
import eu.kevin.demo.extensions.replaceDecimalSeparatorWithDot
import eu.kevin.demo.extensions.setDebounceClickListener
import eu.kevin.demo.helpers.PaymentTypeHelper
import eu.kevin.demo.helpers.SpannableStringHelper
import eu.kevin.demo.helpers.SpannableStringLink
import eu.kevin.demo.main.adapter.CreditorsAdapter
import eu.kevin.demo.main.entities.DonationRequest
import eu.kevin.demo.main.entities.ValidationResult
import eu.kevin.demo.main.entities.exceptions.CreditorNotSelectedException
import eu.kevin.demo.views.NumberTextWatcher
import eu.kevin.inapppayments.paymentsession.enums.PaymentType

internal class MainView(context: Context) : FrameLayout(context) {

    var callback: MainViewCallback? = null

    private val binding = FragmentMainBinding.inflate(LayoutInflater.from(context), this)
    private val creditorsAdapter = CreditorsAdapter {
        callback?.onCreditorSelected(it)
    }

    init {
        with(binding) {
            creditorsRecyclerView.adapter = creditorsAdapter

            termsTextView.text = SpannableStringHelper.getSpannableWithLinks(
                context.getString(R.string.window_main_terms_privacy_policy),
                ContextCompat.getColor(context, R.color.blue),
                SpannableStringLink(context.getString(R.string.window_main_terms_privacy_policy_clickable_terms)) {
                    callback?.openUrl(
                        context.getString(R.string.terms_url)
                    )
                },
                SpannableStringLink(context.getString(R.string.window_main_terms_privacy_policy_clickable_policy)) {
                    callback?.openUrl(
                        context.getString(R.string.privacy_policy_url)
                    )
                }
            )
            termsTextView.movementMethod = LinkMovementMethod()

            paymentTypeSelectionBar.setItems(
                PaymentType.values().map {
                    context.getString(PaymentTypeHelper.getStringRes(it))
                }
            )

            amountTextField.editText?.addTextChangedListener(
                NumberTextWatcher(
                    amountTextField.editText!!,
                    getCurrentLocale(),
                    2
                )
            )
        }
        initListeners()

        binding.scrollView.applySystemInsetsPadding(bottom = true)
        KeyboardManager(binding.root).apply {
            onKeyboardSizeChanged {
                binding.root.updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = it
                }
            }
        }
    }

    fun update(state: MainViewState) {
        creditorsAdapter.update(state.creditors)
        when (state.loadingState) {
            is LoadingState.Loading -> startLoading(state.loadingState.isLoading())
            is LoadingState.FailureWithMessage -> showError(state.loadingState.message)
            is LoadingState.Failure -> {
                showError(getErrorMessage(state.loadingState.error))
            }
        }
        with(binding) {
            proceedButton.text =
                context.getString(R.string.window_main_proceed_button, state.buttonText)

            countryFlagImageView.setImageDrawable(
                CountryHelper.getCountryFlagDrawable(
                    context,
                    state.selectedCountry
                )
            )
            selectedCountryTextView.text =
                CountryHelper.getCountryName(context, state.selectedCountry)
            creditorsRecyclerView.isInvisible = state.loadingCreditors
            creditorsProgressBar.isGone = !state.loadingCreditors
        }
    }

    fun showInputFieldValidations(
        emailValidation: ValidationResult,
        amountValidation: ValidationResult,
        termsAccepted: Boolean
    ) {
        with(binding) {
            emailTextField.error = emailValidation.getMessage(context)
            amountTextField.error = amountValidation.getMessage(context)
            termsErrorImageView.isGone = termsAccepted
        }
    }

    fun showSuccessDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_payment_success_title)
            .setPositiveButton(R.string.dialog_payment_success_button) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun resetFields() {
        with(binding) {
            emailTextField.editText?.setText("")
            amountTextField.editText?.setText("")
            termsCheckbox.isChecked = false
        }
    }

    private fun getErrorMessage(error: Throwable): String {
        return when (error) {
            is CreditorNotSelectedException -> context.getString(R.string.window_main_no_creditor_selected_error)
            else -> ErrorHelper.getMessage(context, error)
        }
    }

    private fun initListeners() {
        with(binding) {
            proceedButton.setOnClickListener {
                hideKeyboard()
                callback?.onDonateClick(
                    DonationRequest(
                        email = binding.emailTextField.getInputText(),
                        amount = binding.amountTextField
                            .getInputText()
                            .removeNumberSeparator(getCurrentLocale())
                            .replaceDecimalSeparatorWithDot(getCurrentLocale()),
                        isTermsAccepted = binding.termsCheckbox.isChecked,
                        paymentType = PaymentType.values()[binding.paymentTypeSelectionBar.getCurrentItemIndex()]
                    )
                )
            }

            termsCheckbox.setOnCheckedChangeListener { _, _ ->
                termsErrorImageView.isGone = true
            }

            emailTextField.editText?.addTextChangedListener {
                emailTextField.error = null
                emailTextField.isErrorEnabled = false
            }

            amountTextField.editText?.addTextChangedListener {
                val text = (it?.toString() ?: "")
                    .removeNumberSeparator(getCurrentLocale())
                    .replaceDecimalSeparatorWithDot(getCurrentLocale())

                callback?.onAmountChanged(text)
                amountTextField.error = null
                amountTextField.isErrorEnabled = false
            }

            countrySelectionContainer.setDebounceClickListener {
                callback?.onSelectCountryClick()
            }
        }
    }

    private fun startLoading(isLoading: Boolean) {
        with(binding) {
            if (isLoading) {
                progressView.fadeIn()
            } else {
                progressView.fadeOut()
            }
        }
    }

    private fun showError(message: String) {
        binding.progressView.fadeOut()
        SnackbarHelper.showError(this, message)
    }
}
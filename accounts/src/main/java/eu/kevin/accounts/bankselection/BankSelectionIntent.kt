package eu.kevin.accounts.bankselection

import eu.kevin.common.architecture.interfaces.IIntent

internal sealed class BankSelectionIntent : IIntent {
    data class Initialize(val configuration: BankSelectionFragmentConfiguration) : BankSelectionIntent()
    data class HandleCountrySelectionClick(val configuration: BankSelectionFragmentConfiguration) : BankSelectionIntent()
    data class HandleCountrySelected(
        val selectedCountry: String,
        val configuration: BankSelectionFragmentConfiguration
    ) : BankSelectionIntent()
    object HandleContinueClicked : BankSelectionIntent()
    object HandleBackClicked : BankSelectionIntent()
    data class HandleBankSelection(val bankId: String) : BankSelectionIntent()
}
package eu.kevin.accounts.bankselection

import android.content.Context
import androidx.fragment.app.viewModels
import eu.kevin.accounts.bankselection.BankSelectionIntent.*
import eu.kevin.accounts.countryselection.CountrySelectionContract
import eu.kevin.common.architecture.BaseFragment
import eu.kevin.common.architecture.interfaces.IView
import eu.kevin.common.extensions.setFragmentResultListener

internal class BankSelectionFragment : BaseFragment<BankSelectionState, BankSelectionIntent, BankSelectionViewModel>(),
    BankSelectionViewDelegate {

    var configuration: BankSelectionFragmentConfiguration? by savedState()

    override val viewModel: BankSelectionViewModel by viewModels {
        BankSelectionViewModel.Factory(this)
    }

    override fun onCreateView(context: Context): IView<BankSelectionState> {
        return BankSelectionView(context).also {
            it.delegate = this
        }
    }

    override fun onAttached() {
        viewModel.intents.trySend(Initialize(configuration!!))
        parentFragmentManager.setFragmentResultListener(CountrySelectionContract, this) {
            viewModel.intents.trySend(HandleCountrySelected(it, configuration!!))
        }
    }

    override fun onBackPressed(): Boolean {
        viewModel.intents.trySend(HandleBackClicked)
        return true
    }

    // BankSelectionViewDelegate

    override fun onBackClicked() {
        viewModel.intents.trySend(HandleBackClicked)
    }

    override fun onBankClicked(bankId: String) {
        viewModel.intents.trySend(HandleBankSelection(bankId))
    }

    override fun onSelectCountryClicked() {
        viewModel.intents.trySend(HandleCountrySelectionClick(configuration!!))
    }

    override fun onContinueClicked() {
        viewModel.intents.trySend(HandleContinueClicked)
    }
}
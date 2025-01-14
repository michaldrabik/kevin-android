package eu.kevin.accounts.bankselection.adapters

import android.view.LayoutInflater
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import coil.request.ImageRequest
import eu.kevin.accounts.bankselection.entities.BankListItem
import eu.kevin.accounts.databinding.ViewBankListItemBinding
import eu.kevin.common.architecture.BaseListAdapter
import eu.kevin.common.extensions.isDarkMode
import eu.kevin.common.extensions.setDebounceClickListener

internal class BankListAdapter(
    override var items: List<BankListItem> = emptyList(),
    private val onBankClicked: (String) -> Unit
) : BaseListAdapter<BankListItem, ViewBankListItemBinding>(items) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ViewBankListItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(binding: ViewBankListItemBinding, item: BankListItem, position: Int) {
        with(binding) {
            root.setDebounceClickListener {
                onBankClicked.invoke(item.bankId)
            }
            root.contentDescription = item.title
            bankTitleView.text = item.title
            loadBankImage(binding, item.logoUrl)
            root.isSelected = item.isSelected
        }
    }

    override fun updateItems(items: List<BankListItem>) {
        val diffResult = DiffUtil.calculateDiff(BankListDiffCallback(this.items, items))
        this.items = items
        diffResult.dispatchUpdatesTo(this)
    }

    private fun loadBankImage(binding: ViewBankListItemBinding, logoUrl: String) {
        val url = if (binding.root.context.isDarkMode()) {
            try {
                val urlParts = logoUrl.split("images/")
                "${urlParts[0]}images/white/${urlParts[1]}"
            } catch (e: Exception) {
                logoUrl
            }
        } else {
            logoUrl
        }
        with(binding) {
            val imageRequest = ImageRequest.Builder(root.context)
                .data(url)
                .target(
                    onSuccess = {
                        bankImageView.setImageDrawable(it)
                        bankTitleView.visibility = INVISIBLE
                        bankImageView.visibility = VISIBLE
                    },
                    onError = {
                        bankTitleView.visibility = VISIBLE
                        bankImageView.visibility = INVISIBLE
                    }
                )
                .build()
            ImageLoader.invoke(root.context).enqueue(imageRequest)
        }
    }
}
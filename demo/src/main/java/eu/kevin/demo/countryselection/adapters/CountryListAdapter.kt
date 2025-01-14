package eu.kevin.demo.countryselection.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import eu.kevin.common.extensions.getDrawableCompat
import eu.kevin.common.extensions.setDebounceClickListener
import eu.kevin.demo.R
import eu.kevin.demo.countryselection.entities.Country
import eu.kevin.demo.countryselection.helpers.CountryHelper
import eu.kevin.demo.databinding.ItemCountryListBinding

internal class CountryListAdapter(
    override var items: List<Country> = emptyList(),
    private val onCountryClicked: (String) -> Unit
) : BaseListAdapter<Country, ItemCountryListBinding>(items) {

    override fun onBindingRequested(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): ItemCountryListBinding {
        return ItemCountryListBinding.inflate(inflater, parent, false)
    }

    override fun onBindViewHolder(binding: ItemCountryListBinding, item: Country, position: Int) {
        val background = when (position) {
            0 -> context.getDrawableCompat(R.drawable.country_selection_background_top)
            items.size - 1 -> context.getDrawableCompat(R.drawable.country_selection_background_bottom)
            else -> context.getDrawableCompat(R.drawable.country_selection_background_middle)
        }
        val foreground = when (position) {
            0 -> context.getDrawableCompat(R.drawable.country_selection_ripple_top)
            items.size - 1 -> context.getDrawableCompat(R.drawable.country_selection_ripple_bottom)
            else -> context.getDrawableCompat(R.drawable.country_selection_ripple_middle)
        }
        with(binding) {
            root.setDebounceClickListener {
                onCountryClicked.invoke(item.iso)
            }
            root.isSelected = item.isSelected
            root.background = background
            root.foreground = foreground
            countryTextView.text = item.title
            countryFlagImageView.setImageDrawable(CountryHelper.getCountryFlagDrawable(context, item.iso))
        }
    }

    override fun updateItems(items: List<Country>) {
        val diffResult = DiffUtil.calculateDiff(CountryListDiffCallback(this.items, items))
        this.items = items
        diffResult.dispatchUpdatesTo(this)
    }
}
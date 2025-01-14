package eu.kevin.demo.countryselection.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import eu.kevin.accounts.R

internal object CountryHelper {

    fun getCountryFlagDrawable(context: Context, countryIso: String): Drawable {
        val resourceId = context.resources.getIdentifier(
            "ic_country_flag_${countryIso.lowercase()}", "drawable",
            context.packageName
        )
        if (resourceId == 0) {
            return ContextCompat.getDrawable(context, R.drawable.ic_country_flag_eu)!!
        }
        return ContextCompat.getDrawable(context, resourceId)!!
    }

    fun getCountryName(context: Context, countryIso: String): String {
        val resourceId = context.resources.getIdentifier(
            "country_name_${countryIso.lowercase()}", "string",
            context.packageName
        )
        if (resourceId == 0) {
            return countryIso
        }
        return context.getString(resourceId)
    }
}
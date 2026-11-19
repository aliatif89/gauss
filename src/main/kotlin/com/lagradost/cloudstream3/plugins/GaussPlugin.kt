package com.lagradost.cloudstream3.plugins

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.widget.EditText
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class GaussPlugin : Plugin() {

    override fun load(context: Context) {
        val provider = HdFilmCehennemiProvider()

        val preferences = context.getSharedPreferences(
            "gauss_settings",
            Context.MODE_PRIVATE
        )

        val savedUrl = preferences.getString(
            "hdfilmcehennemi_url",
            null
        )

        if (!savedUrl.isNullOrBlank()) {
            val customUrl = savedUrl.trim().trimEnd('/')
            provider.userConfiguredUrl = customUrl
            provider.mainUrl = customUrl
        }

        registerMainAPI(provider)

        openSettings = { activity ->
            val input = EditText(activity).apply {
                setText(provider.mainUrl)
                hint = "https://example.com"
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_URI
                singleLine = true
            }

            AlertDialog.Builder(activity)
                .setTitle("GAUSS Kaynak Ayarları")
                .setMessage("HDFilmCehennemi site adresi")
                .setView(input)
                .setPositiveButton("Kaydet") { _, _ ->
                    val newUrl = input.text.toString()
                        .trim()
                        .trimEnd('/')

                    if (
                        newUrl.startsWith("https://") ||
                        newUrl.startsWith("http://")
                    ) {
                        preferences.edit()
                            .putString("hdfilmcehennemi_url", newUrl)
                            .apply()

                        provider.userConfiguredUrl = newUrl
                        provider.mainUrl = newUrl
                    }
                }
                .setNeutralButton("Varsayılana dön") { _, _ ->
                    preferences.edit()
                        .remove("hdfilmcehennemi_url")
                        .apply()

                    provider.userConfiguredUrl = null
                    provider.mainUrl = "https://www.hdfilmcehennemi.nl"
                }
                .setNegativeButton("Kapat", null)
                .show()
        }
    }
}

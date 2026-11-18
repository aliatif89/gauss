
package com.lagradost.cloudstream3.plugins

import android.content.Context
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class GaussPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(HdFilmCehennemiProvider())
    }
}

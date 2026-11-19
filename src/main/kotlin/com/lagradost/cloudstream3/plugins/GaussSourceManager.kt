package com.lagradost.cloudstream3.plugins

import com.lagradost.cloudstream3.app
import org.json.JSONObject

object GaussSourceManager {

    private const val CONFIG_URL =
        "https://raw.githubusercontent.com/aliatif89/gauss/main/config/sources.json"

    private const val CACHE_DURATION = 5 * 60 * 1000L

    private var lastFetchTime = 0L
    private var cachedUrl: String? = null

    suspend fun refreshHDFilmCehennemi(
        provider: HdFilmCehennemiProvider
    ) {
        val now = System.currentTimeMillis()

        if (
            cachedUrl != null &&
            now - lastFetchTime < CACHE_DURATION
        ) {
            provider.mainUrl = cachedUrl!!
            return
        }

        try {
            val response = app.get(CONFIG_URL)
            val json = JSONObject(response.text)
            val sources = json.getJSONArray("sources")

            for (i in 0 until sources.length()) {
                val source = sources.getJSONObject(i)

                if (
                    source.optString("id") == "hdfilmcehennemi" &&
                    source.optBoolean("enabled", false)
                ) {
                    val url = source.optString("baseUrl")
                        .trim()
                        .trimEnd('/')

                    if (
                        url.startsWith("https://") ||
                        url.startsWith("http://")
                    ) {
                        cachedUrl = url
                        lastFetchTime = now
                        provider.mainUrl = url
                    }

                    return
                }
            }
        } catch (_: Exception) {
            // GitHub erişilemezse mevcut adres korunur.
        }
    }
}

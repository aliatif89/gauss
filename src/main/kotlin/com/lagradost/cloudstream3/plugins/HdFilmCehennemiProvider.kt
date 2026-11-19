package com.lagradost.cloudstream3.plugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class HdFilmCehennemiProvider : MainAPI() {

    override var mainUrl = "https://www.hdfilmcehennemi.nl"

    var userConfiguredUrl: String? = null

    override var name = "HdFilmCehennemi"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "tr"
    override var hasMainPage = true

    private suspend fun ensureSourceUrl() {
        val customUrl = userConfiguredUrl

        if (!customUrl.isNullOrBlank()) {
            mainUrl = customUrl
            return
        }

        GaussSourceManager.refreshHDFilmCehennemi(this)
    }

    private fun safeFixUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""
        return fixUrl(trimmed)
    }

    private suspend fun invokeCloudstreamExtractor(
        url: String,
        referer: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            val clazz = Class.forName("com.lagradost.cloudstream3.utils.ExtractorApiKt")
            val method = clazz.methods.firstOrNull { m ->
                m.name == "loadExtractor" && m.parameterCount >= 4
            }
            if (method != null) {
                val continuation = kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn<Any?> { cont ->
                    method.invoke(null, url, referer, subtitleCallback, callback, cont)
                }
                if (continuation === kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED) {
                    return
                }
            }
        } catch (_: Exception) {
            // Derleme/çalışma ortamında extractor bulunamazsa sessizce geçilir.
        }
    }

    // ANA SAYFA

    override suspend fun getMainPage(
        page: Int,
        request: HomePageRequest
    ): HomePageResponse {
        ensureSourceUrl()

        val document = app.get(mainUrl).document
        val home = ArrayList<HomePageList>()

        val elements = document.select(
            "div.col-xl-2, div.film-box"
        )

        val list = elements.mapNotNull { element ->
            element.toSearchResult()
        }

        if (list.isNotEmpty()) {
            home.add(
                HomePageList("Son Eklenenler", list)
            )
        }

        return newHomePageResponse(home, false)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.select(
            "a.title, h3 a, .film-title"
        ).text().trim()

        val poster = this.select("img")
            .attr("data-src")
            .ifEmpty {
                this.select("img").attr("src")
            }

        val href = safeFixUrl(
            this.select("a").attr("href")
        )

        if (title.isEmpty() || href.isEmpty()) {
            return null
        }

        return newMovieSearchResponse(
            title,
            href,
            TvType.Movie
        ) {
            this.posterUrl = poster
        }
    }

    // ARAMA

    override suspend fun search(
        query: String
    ): List<SearchResponse> {
        ensureSourceUrl()

        val searchUrl = "$mainUrl/arama?q=$query"
        val document = app.get(searchUrl).document

        return document.select(
            "div.film-box, div.col-xl-2"
        ).mapNotNull { element ->
            element.toSearchResult()
        }
    }

    // FİLM VE DİZİ DETAYLARI

    override suspend fun load(
        url: String
    ): LoadResponse {
        ensureSourceUrl()

        val document = app.get(url).document

        val title = document.select(
            "h1.film-name, h1"
        ).text().trim()

        val poster = document.select(
            ".poster img, .film-poster img"
        ).attr("src")

        val description = document.select(
            ".film-summary, .ozet"
        ).text().trim()

        val year = document.select(
            ".yil, .year"
        ).text().toIntOrNull()

        val isSeries =
            url.contains("/dizi/") ||
            document.select(".bolum-listesi").isNotEmpty()

        if (isSeries) {
            val episodes = ArrayList<Episode>()

            document.select(
                ".bolum-secenekleri a, .episodelist a"
            ).forEachIndexed { index, element ->
                val epName = element.text().trim()
                val epUrl = safeFixUrl(
                    element.attr("href")
                )

                if (epUrl.isNotEmpty()) {
                    episodes.add(
                        Episode(
                            data = epUrl,
                            name = epName,
                            episode = index + 1,
                            season = 1
                        )
                    )
                }
            }

            return newTvSeriesLoadResponse(
                title,
                url,
                TvType.TvSeries,
                episodes
            ) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
            }
        } else {
            return newMovieLoadResponse(
                title,
                url,
                TvType.Movie
            ) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
            }
        }
    }

    // VİDEO LİNKLERİ

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        ensureSourceUrl()

        val document = app.get(data).document

        val videoIframeUrl = document.select(
            "iframe.embed-responsive-item, iframe#playerElement"
        ).attr("src")

        val resolvedUrl = safeFixUrl(videoIframeUrl)
        if (resolvedUrl.isNotEmpty()) {
            invokeCloudstreamExtractor(
                resolvedUrl,
                data,
                subtitleCallback,
                callback
            )
        }

        return true
    }
}

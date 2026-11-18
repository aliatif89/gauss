package com.lagradost.cloudstream3.plugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class HdFilmCehennemiProvider : MainAPI() {
    override var mainUrl = "https://www.hdfilmcehennemi.nl"
    
    override var name = "HdFilmCehennemi"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "tr"
    override var hasMainPage = true

    // 1. ANA SAYFA FİLMLERİNİ ÇEKME
    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val home = ArrayList<HomePageList>()

        val elements = document.select("div.col-xl-2, div.film-box") 
        val list = elements.mapNotNull { element ->
            element.toSearchResult()
        }

        if (list.isNotEmpty()) {
            home.add(HomePageList("Son Eklenenler", list))
        }

        return newHomePageResponse(home, false)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.select("a.title, h3 a, .film-title").text().trim()
        val poster = this.select("img").attr("data-src").ifEmpty { 
            this.select("img").attr("src") 
        }
        val href = fixUrl(this.select("a").attr("href"))

        if (title.isEmpty() || href.isEmpty()) return null

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    // 2. ARAMA YAPMA
    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/arama?q=$query"
        val document = app.get(searchUrl).document

        return document.select("div.film-box, div.col-xl-2").mapNotNull { element ->
            element.toSearchResult()
        }
    }

    // 3. FİLM / DİZİ DETAYLARI VE SEÇENEKLER (GÜNCEL LOAD)
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        
        val title = document.select("h1.film-name, h1").text().trim()
        val poster = document.select(".poster img, .film-poster img").attr("src")
        val description = document.select(".film-summary, .ozet").text().trim()
        val year = document.select(".yil, .year").text().toIntOrNull()

        val isSeries = url.contains("/dizi/") || document.select(".bolum-listesi").isNotEmpty()

        if (isSeries) {
            val episodes = ArrayList<Episode>()
            document.select(".bolum-secenekleri a, .episodelist a").forEachIndexed { index, element ->
                val epName = element.text().trim()
                val epUrl = fixUrl(element.attr("href"))
                
                episodes.add(
                    Episode(
                        data = epUrl,
                        name = epName,
                        episode = index + 1,
                        season = 1
                    )
                )
            }

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
            }
        } else {
            val episodes = ArrayList<Episode>()
            val altLinks = document.select(".alternatif-secenekleri a, .dil-secenekleri button")
            
            if (altLinks.isNotEmpty()) {
                altLinks.forEachIndexed { index, element ->
                    val langName = element.text().trim()
                    episodes.add(
                        Episode(
                            data = url,
                            name = langName,
                            episode = index + 1
                        )
                    )
                }
            } else {
                episodes.add(Episode(data = url, name = "Tek Parça", episode = 1))
            }

            return newMovieLoadResponse(title, url, TvType.Movie, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
            }
        }
    }

    // 4. VİDEO LİNKLERİNİ ÇÖZME
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val videoIframeUrl = document.select("iframe.embed-responsive-item, iframe#playerElement").attr("src")

        if (videoIframeUrl.isNotEmpty()) {
            loadExtractor(fixUrl(videoIframeUrl), data, subtitleCallback, callback)
        }

        return true
    }
}

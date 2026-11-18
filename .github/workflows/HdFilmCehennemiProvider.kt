package com.lagradost.cloudstream3.plugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class HdFilmCehennemiProvider : MainAPI() {
    // Site adresi değiştiğinde sadece bu satırı güncellemen yeterlidir:
    override var mainUrl = "https://www.hdfilmcehennemi.nl"
    
    override var name = "HdFilmCehennemi"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "tr"
    override var hasMainPage = true

    // 1. ANA SAYFA FİLMLERİNİ ÇEKME
    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val home = ArrayList<HomePageList>()

        // Sitedeki film kartlarını seçiyoruz (Örnek seçici, site yapısına göre ufak değişebilir)
        val elements = document.select("div.col-xl-2, div.film-box") 
        
        val list = elements.mapNotNull { element ->
            element.toSearchResult()
        }

        if (list.isNotEmpty()) {
            home.add(HomePageList("Son Eklenen Filmler", list))
        }

        return newHomePageResponse(home, false)
    }

    // HTML Elemanını CloudStream Arama/Ana Sayfa Sonucuna Dönüştürme Yardımcısı
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

    // 2. ARAMA YAPMA FONKSİYONU
    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/arama?q=$query"
        val document = app.get(searchUrl).document

        return document.select("div.film-box, div.col-xl-2").mapNotNull { element ->
            element.toSearchResult()
        }
    }

    // 3. FİLM DETAYLARI, DUBLAJ/ALTYAZI VE VİDEO KAYNAKLARI (LOAD)
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        
        val title = document.select("h1.film-name, h1").text().trim()
        val poster = document.select(".poster img, .film-poster img").attr("src")
        val description = document.select(".film-summary, .ozet").text().trim()
        val year = document.select(".yil, .year").text().toIntOrNull()

        // Dublaj ve Altyazı seçeneklerini burada bölüm/alternatif olarak ekleyeceğiz
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
        }
    }

    // 4. VİDEO OYNATMA LİNKLERİNİ ÇÖZME (LOAD LINKS)
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        // Sitenin içindeki video oynatıcı kaynaklarını (iframe / m3u8 / mp4) burada yakalayıp callback ile oynatıcıya veriyoruz.
        // CloudStream'in yerleşik extractor'ları (loadExtractor vb.) burada devreye girecek.
        
        return true
    }
}

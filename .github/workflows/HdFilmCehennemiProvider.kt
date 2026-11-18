package com.lagradost.cloudstream3.plugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class HdFilmCehennemiProvider : MainAPI() {
    // Site adresi değiştiğinde buradaki adresi güncellemen yeterlidir:
    override var mainUrl = "https://www.hdfilmcehennemi.nl"
    
    override var name = "HdFilmCehennemi"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "tr"
    override var hasMainPage = true

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageResponse {
        val document = app.get("$mainUrl/gelen-filmler").document
        // Film listeleme mantığı buraya yazılacak
        return newHomePageResponse(emptyList(), false)
    }
}

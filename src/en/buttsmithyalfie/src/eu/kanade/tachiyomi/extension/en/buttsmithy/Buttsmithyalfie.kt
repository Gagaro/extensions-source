package eu.kanade.tachiyomi.extension.en.buttsmithyalfie

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Locale

class Buttsmithyalfie : HttpSource() {

    override val name = "Buttsmithy Alfie"

    override val baseUrl = "https://buttsmithy.com"

    // the full version of alfie for some reason has a separate url and isn't accessed like the other comics
    private val baseUrlAlfie = "https://buttsmithy.com"
    private val chapterOverviewBaseUrl = "$baseUrlAlfie/archives/chapter"

    override val lang = "en"

    private val inCase = "InCase"
    private val alfieTitle = "Alfie"
    private val alfieDateParser = SimpleDateFormat("HH:mm MMMM dd, yyyy", Locale.US)

    override val supportsLatest: Boolean = false

    private fun String.lowercase(): String {
        return this.lowercase(Locale.getDefault())
    }

    /**
     * Fetches all chapters of Alfie (one of InCases comics) as separate SManga because this comic
     * is gigantic and only updates one page at a time. Putting the pages into SChapters would block
     * the automatic update fetching.
     *
     * @return all of Alfies chapters as separate SManga
     */
    private fun fetchAlfieSMangas(): List<SManga> {
        return listOf(
            SManga.create().apply {
                url = baseUrl
                title = alfieTitle
                author = inCase
                artist = inCase
                status = SManga.COMPLETED
                genre = "fantasy, NSFW"
                thumbnail_url = generateImageUrlWithText(alfieTitle)
            },
        )
    }

    private fun generateImageUrlWithText(text: String): String {
        return "https://fakeimg.pl/800x1236/?text=$text&font=lobster"
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val currentDoc = client.newCall(GET(baseUrl, headers)).execute().asJsoup()

        val allChapters = currentDoc.select("#chapter option.level-0")
            .map { chapterElement ->
                var chapUrl = chapterElement.attr("value")
                var title = chapterElement.text()

                SChapter.create().apply {
                    url = chapUrl
                    name = title
                    chapter_number = if (title == "Misc") {
                        0f
                    } else {
                        title.substringAfter(" ").trim().toFloat()
                    }
                }
            }.sortedBy { it.chapter_number }.reversed()

        return Observable.just(allChapters)
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        val comicChapterDoc = client.newCall(GET(chapter.url, headers)).execute().asJsoup()

        var allPages = comicChapterDoc.select("#comic-head .comic-nav-jumptocomic option.level-0")
            .mapIndexed { index, pageElement ->
                val comicPageDoc = client.newCall(GET(pageElement.attr("value"), headers)).execute().asJsoup()
                val imageUrl = comicPageDoc.select("#comic img").attr("src")

                Page(index, "", imageUrl)
            }

        return Observable.just(allPages)
    }

    override fun imageUrlParse(response: Response): String {
        val pageDoc = response.asJsoup()
        return pageDoc.select("#comic").select("img[src]").attr("href")
    }

    private fun generateMangasPage(): MangasPage {
        return MangasPage(fetchAlfieSMangas(), false)
    }

    override fun chapterListParse(response: Response): List<SChapter> = throw UnsupportedOperationException()

    override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException()

    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return Observable.just(manga)
    }

    override fun mangaDetailsRequest(manga: SManga): Request = GET(manga.url, headers)

    override fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException()

    override fun pageListParse(response: Response): List<Page> = throw UnsupportedOperationException()

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        return Observable.just(generateMangasPage())
    }

    override fun popularMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()

    override fun popularMangaRequest(page: Int): Request = throw UnsupportedOperationException()

    override fun searchMangaParse(response: Response): MangasPage = throw UnsupportedOperationException()

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        throw UnsupportedOperationException()
}

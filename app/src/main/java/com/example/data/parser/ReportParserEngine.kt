package com.example.data.parser

import android.util.Log
import com.example.data.model.ElectricalReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Capa de red: descarga el canal y delega la interpretación en [ReportTextParser]. */
class ReportParserEngine(private val parser: ReportTextParser = ReportTextParser()) {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * Descarga el canal y devuelve los comunicados recientes de la más antigua a la más reciente
     * (el último elemento es siempre el más nuevo).
     */
    suspend fun fetchChannelReports(url: String): Result<List<ElectricalReport>> = withContext(Dispatchers.IO) {
        val telegramUrl = "https://t.me/s/une_eeg"

        fun tryFetch(u: String, isFallback: Boolean): Result<List<ElectricalReport>> {
            try {
                val isTelegram = u.contains("t.me/")
                val formattedUrl = if (isTelegram && !u.contains("t.me/s/")) u.replace("t.me/", "t.me/s/") else u

                val request = Request.Builder()
                    .url(formattedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Mobile Safari/537.36")
                    .build()

                val html = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("El servidor respondió HTTP ${response.code}")
                    response.body?.string().orEmpty()
                }

                // Feed HTML dedicado de tg.i-c-a.su (vía rss2json o directo)
                if (html.contains("<article class=\"item\">")) {
                    val feedItems = when {
                        u.contains("rss2json.com") || u.contains("api.json") -> {
                            val root = JSONObject(html)
                            val itemsArray = root.optJSONArray("items") ?: return@tryFetch Result.success(emptyList())
                            val contents = (0 until itemsArray.length()).mapNotNull { i ->
                                itemsArray.getJSONObject(i).optString("content", null)
                            }
                            FeedHtmlParser().parse(contents.joinToString("\n"))
                        }
                        else -> FeedHtmlParser().parse(html)
                    }
                    if (feedItems.isEmpty()) {
                        if (!isFallback && (u.contains("rss2json.com") || u.contains("api.json"))) {
                            return tryFetch(telegramUrl, true)
                        }
                        return@tryFetch Result.success(emptyList())
                    }
                    val reports = FeedHtmlParser().toReports(feedItems, sourceUrl = u)
                    if (reports.isEmpty()) {
                        if (!isFallback && (u.contains("rss2json.com") || u.contains("api.json"))) {
                            return tryFetch(telegramUrl, true)
                        }
                        return@tryFetch Result.success(emptyList())
                    }
                    return@tryFetch Result.success(reports)
                }

                val posts = when {
                    u.contains("rss2json.com") || u.contains("api.json") -> parseRss2Json(html)
                    TelegramChannelParser.looksLikeChannelView(html) -> TelegramChannelParser.parse(html)
                    isTelegram -> throw IOException("El canal no tiene vista pública (t.me/s/) o no se pudo leer")
                    else -> TelegramChannelParser.plainText(html).takeIf { it.length > 50 }
                        ?.let { listOf(ChannelPost(id = null, text = it.take(3000), publishedAtMs = null)) }
                        .orEmpty()
                }
                if (posts.isEmpty()) {
                    if (!isFallback && (u.contains("rss2json.com") || u.contains("api.json"))) {
                        return tryFetch(telegramUrl, true)
                    }
                    throw IOException("No se encontraron publicaciones con texto en la fuente")
                }

                Result.success(parser.reportsFromPosts(posts, sourceUrl = u))
            } catch (e: Exception) {
                if (!isFallback && (u.contains("rss2json.com") || u.contains("api.json"))) {
                    return tryFetch(telegramUrl, true)
                }
                Log.e("ReportParserEngine", "Error fetching channel${if (isFallback) " (fallback)" else ""}", e)
                Result.failure(e)
            }
            // Should never reach here, but satisfies compiler
            return@tryFetch Result.failure(IllegalStateException("tryFetch completed without returning"))
        }

        tryFetch(url, false)
    }

    /** Parsea la respuesta JSON de rss2json y devuelve los posts de más antigua a más reciente. */
    private fun parseRss2Json(html: String): List<ChannelPost> {
        val root = JSONObject(html)
        if (root.optString("status", "") != "ok") {
            throw IOException("La API respondió: ${root.optString("message", "error desconocido")}")
        }
        val items = root.optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull { i ->
            val item = items.getJSONObject(i)
            val id = item.optString("guid", null).takeIf { it.isNotEmpty() }
                ?: item.optString("link", null).takeIf { it.isNotEmpty() }
            val rawText = item.optString("content", null)
                ?: item.optString("description", "")
                ?: item.optString("title", "")
            val text = TelegramChannelParser.plainText(rawText)
            if (text.isBlank()) null
            else {
                val pubDate = item.optString("isoDate", null) ?: item.optString("pubDate", null)
                ChannelPost(
                    id = id,
                    text = text,
                    publishedAtMs = parseRssDate(pubDate)
                )
            }
        }
    }

    private fun parseRssDate(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (fmt in formats) {
            val parsed = try {
                SimpleDateFormat(fmt, Locale.US).parse(value)?.time
            } catch (e: Exception) {
                null
            }
            if (parsed != null) return parsed
        }
        return null
    }

    fun parseReportText(text: String, sourceUrl: String = "Texto ingresado"): ElectricalReport =
        parser.parse(text, sourceUrl)
}

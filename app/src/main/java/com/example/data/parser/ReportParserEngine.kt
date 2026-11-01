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
        try {
            val isTelegram = url.contains("t.me/")
            val formattedUrl = if (isTelegram && !url.contains("t.me/s/")) url.replace("t.me/", "t.me/s/") else url

            val request = Request.Builder()
                .url(formattedUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Mobile Safari/537.36")
                .build()

            val html = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("El servidor respondió HTTP ${response.code}")
                response.body?.string().orEmpty()
            }

            val posts = when {
                url.contains("rss2json.com") || url.contains("api.json") -> parseRss2Json(html)
                TelegramChannelParser.looksLikeChannelView(html) -> TelegramChannelParser.parse(html)
                isTelegram -> throw IOException("El canal no tiene vista pública (t.me/s/) o no se pudo leer")
                else -> TelegramChannelParser.plainText(html).takeIf { it.length > 50 }
                    ?.let { listOf(ChannelPost(id = null, text = it.take(3000), publishedAtMs = null)) }
                    .orEmpty()
            }
            if (posts.isEmpty()) throw IOException("No se encontraron publicaciones con texto en la fuente")

            Result.success(parser.reportsFromPosts(posts, sourceUrl = url))
        } catch (e: Exception) {
            Log.e("ReportParserEngine", "Error fetching channel", e)
            Result.failure(e)
        }
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

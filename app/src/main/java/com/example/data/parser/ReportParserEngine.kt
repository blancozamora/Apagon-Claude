package com.example.data.parser

import android.util.Log
import com.example.data.model.ElectricalReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
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

    fun parseReportText(text: String, sourceUrl: String = "Texto ingresado"): ElectricalReport =
        parser.parse(text, sourceUrl)
}

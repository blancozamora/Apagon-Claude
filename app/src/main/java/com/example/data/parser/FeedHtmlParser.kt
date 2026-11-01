package com.example.data.parser

import com.example.data.model.CircuitInfo
import com.example.data.model.CircuitStatus
import com.example.data.model.CircuitUpdate
import com.example.data.model.ElectricalReport
import com.example.data.provider.GranmaDataProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Parser dedicado al feed HTML de https://tg.i-c-a.su/rss/UNE_EEG.
 * Extrae artículos con listas de circuitos por estado (afectados/restablecidos/rotación)
 * y los resuelve contra el catálogo de circuitos.
 */
class FeedHtmlParser(private val catalog: List<CircuitInfo> = GranmaDataProvider.getDefaultCircuits()) {

    private data class FeedItem(
        val title: String,
        val postId: String?,
        val publishedAtMs: Long?,
        val mwAffected: Int?,
        val mwService: Int?,
        val outageNames: List<String>,
        val energizedNames: List<String>,
        val rotationNames: List<String>,
        val rawHtml: String
    )

    fun parse(html: String): List<FeedItem> {
        val articles = html.split("<article class=\"item\">").drop(1).mapNotNull { chunk ->
            val end = chunk.indexOf("</article>")
            if (end < 0) null else chunk.substring(0, end)
        }
        return articles.map { parseArticle(it) }
    }

    private fun parseArticle(html: String): FeedItem {
        val title = extractFirst(html, """<h2>(.*?)</h2>""")
            ?.let { stripTags(it).trim() }
            ?: "Actualización del Sistema Electroenergético"
        val postId = extractFirst(html, """t\.me/une_eeg/(\d+)""")
        val dateStr = extractFirst(html, """<time datetime="([^"]+)"""")
        val publishedAtMs = parseFeedDate(dateStr)

        val mwAffected = extractMw(html, """<strong>MW afectados:</strong>""")
        val mwService = extractMw(html, """<strong>MW con servicio:</strong>""")

        val outageNames = extractListSection(html, """<p class="bad">""")
        val energizedNames = extractListSection(html, """<p class="ok">""")
        val rotationNames = extractListSection(html, """<p class="warn">""")

        return FeedItem(title, postId, publishedAtMs, mwAffected, mwService, outageNames, energizedNames, rotationNames, html)
    }

    private fun extractFirst(html: String, pattern: String): String? =
        Regex(pattern, RegexOption.DOT_MATCHES_ALL).find(html)?.groupValues?.getOrNull(1)

    private fun extractMw(html: String, label: String): Int? {
        val m = Regex(Regex.escape(label) + r"""\s*</strong>\s*(\d+)""", RegexOption.DOT_MATCHES_ALL).find(html)
            ?: Regex(Regex.escape(label) + r"""\s*(\d+)""", RegexOption.DOT_MATCHES_ALL).find(html)
        return m?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun extractListSection(html: String, headerPattern: String): List<String> {
        val headerIdx = html.indexOf(headerPattern)
        if (headerIdx < 0) return emptyList()
        val fromHeader = html.substring(headerIdx)
        val ulIdx = fromHeader.indexOf("<ul>")
        if (ulIdx < 0) return emptyList()
        val fromUl = fromHeader.substring(ulIdx + 4)
        val endUl = fromUl.indexOf("</ul>")
        if (endUl < 0) return emptyList()
        val ulContent = fromUl.substring(0, endUl)
        return Regex("""<li>(.*?)</li>""", RegexOption.DOT_MATCHES_ALL).findAll(ulContent)
            .mapNotNull { it.groupValues.getOrNull(1) }
            .map { stripTags(it).trim() }
            .filter { it.isNotBlank() }
    }

    private fun stripTags(text: String): String =
        text.replace(Regex("<[^>]*>"), " ").replace(Regex("\s+"), " ").trim()

    private fun parseFeedDate(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "EEE, dd MMM yyyy HH:mm:ss Z",
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

    /** Convierte los items del feed en ElectricalReport listos para aplicar al catálogo. */
    fun toReports(items: List<FeedItem>, sourceUrl: String): List<ElectricalReport> {
        return items.map { item ->
            val circuitUpdates = mutableListOf<CircuitUpdate>()
            val unresolved = mutableListOf<String>()

            val allNames = item.outageNames.map { it to CircuitStatus.OUTAGE } +
                item.energizedNames.map { it to CircuitStatus.ENERGIZED } +
                item.rotationNames.map { it to CircuitStatus.ROTATION }

            for ((name, status) in allNames) {
                val matches = resolveCircuit(name)
                if (matches.isEmpty()) {
                    unresolved += name
                } else {
                    matches.forEach { circuitUpdates += CircuitUpdate(it.id, status) }
                }
            }

            val deficitMw = item.mwAffected ?: item.mwService

            val nowMs = item.publishedAtMs ?: System.currentTimeMillis()
            val dateLabel = if (item.publishedAtMs == null) "Ingresado " else "" + formatDate(nowMs)

            val summary = buildString {
                append("Comunicado del feed oficial. ")
                if (deficitMw != null) append("Déficit de generación: $deficitMw MW. ")
                if (item.outageNames.isNotEmpty()) append("Sin servicio: ${item.outageNames.joinToString(", ")}. ")
                if (item.energizedNames.isNotEmpty()) append("Con servicio: ${item.energizedNames.joinToString(", ")}. ")
                if (item.rotationNames.isNotEmpty()) append("Rotación: ${item.rotationNames.joinToString(", ")}. ")
                if (unresolved.isNotEmpty()) append("No resueltos: ${unresolved.joinToString(", ")}. ")
            }.trim()

            ElectricalReport(
                id = item.postId ?: UUID.randomUUID().toString(),
                date = dateLabel,
                title = item.title,
                rawText = stripTags(item.rawHtml),
                sourceUrl = sourceUrl,
                generationDeficitMW = deficitMw,
                affectedBlocks = item.outageNames,
                energizedCircuits = item.energizedNames,
                outageCircuits = item.outageNames,
                aiSummary = summary,
                timestampMs = nowMs,
                postId = item.postId,
                circuitUpdates = circuitUpdates,
                blockUpdates = emptyList()
            )
        }
    }

    private fun resolveCircuit(name: String): List<CircuitInfo> {
        val clean = cleanFeedName(name)
        val norm = normalize(clean)
        if (norm.isBlank()) return emptyList()

        val exact = catalog.filter { normalize(it.name) == norm }
        if (exact.isNotEmpty()) return exact

        return catalog.filter {
            val cName = normalize(it.name)
            cName.contains(norm) || norm.contains(cName)
        }
    }

    private fun cleanFeedName(name: String): String {
        var n = name.trim()
        n = n.replace(Regex("""\s*\([^)]*\)"""), "")
        n = n.replace(Regex("""\s*o\d+$""", RegexOption.IGNORE_CASE), "")
        return n.trim()
    }

    companion object {
        fun normalize(s: String): String =
            java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replace(Regex("""\p{Mn}+"""), "")
                .lowercase(Locale.ROOT)
                .replace(Regex("""\s+"""), " ")
    }

    private fun formatDate(ms: Long): String =
        SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale("es")).format(Date(ms))
}

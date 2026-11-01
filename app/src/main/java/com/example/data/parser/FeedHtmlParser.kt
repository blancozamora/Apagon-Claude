package com.example.data.parser

import com.example.data.model.CircuitInfo
import com.example.data.model.CircuitStatus
import com.example.data.model.CircuitUpdate
import com.example.data.model.ElectricalReport
import com.example.data.provider.GranmaDataProvider
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.text.RegexOption

/**
 * Parser dedicado al feed HTML de https://tg.i-c-a.su/rss/UNE_EEG.
 * Extrae artículos con listas de circuitos por estado (afectados/restablecidos/rotación)
 * y los resuelve contra el catálogo de circuitos.
 *
 * La resolución de nombres es determinista: coincide el nombre completo del circuito (sin distinguir
 * mayúsculas ni acentos, y conservando códigos como "o813") o figura en [aliases]. Un nombre que no
 * cumple ninguna de las dos cosas queda sin resolver (y se informa en el resumen) en vez de adivinarse.
 */
class FeedHtmlParser(
    private val catalog: List<CircuitInfo> = GranmaDataProvider.getDefaultCircuits(),
    aliases: Map<String, String> = GranmaDataProvider.getFeedAliases()
) {

    /** nombre normalizado -> circuitos del catálogo con ese nombre */
    private val nameIndex: Map<String, List<CircuitInfo>> =
        catalog.groupBy { normalize(cleanFeedName(it.name)) }

    /** nombre del feed (normalizado) -> circuito del catálogo, para los nombres que el catálogo no tiene como propios */
    private val aliasIndex: Map<String, CircuitInfo> = run {
        val byId = catalog.associateBy { it.id }
        aliases.mapNotNull { (feedName, id) -> byId[id]?.let { normalize(cleanFeedName(feedName)) to it } }.toMap()
    }

    fun parse(html: String): List<FeedItem> {
        val articles = html.split("<article class=\"item\">").drop(1).mapNotNull { chunk ->
            val end = chunk.indexOf("</article>")
            if (end < 0) null else chunk.substring(0, end)
        }
        return articles.map { parseArticle(it) }
            .filter { it.publishedAtMs != null }
            .sortedBy { it.publishedAtMs!! }
    }

    private fun parseArticle(html: String): FeedItem {
        val title = extractFirst(html, """<h2>(.*?)</h2>""")
            ?.let { stripTags(it).trim() }
            ?: "Actualización del Sistema Electroenergético"
        val postId = extractFirst(html, """t\.me/une_eeg/(\d+)""")
        val dateStr = extractFirst(html, """<time datetime="([^"]+)"""")
        val publishedAtMs = FeedHtmlParser.parseFeedDate(dateStr)

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
        val escapedLabel = Regex.escape(label)
        val m = Regex(escapedLabel + """\s*</strong>\s*(\d+)""", RegexOption.DOT_MATCHES_ALL).find(html)
            ?: Regex(escapedLabel + """\s*(\d+)""", RegexOption.DOT_MATCHES_ALL).find(html)
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
            .toList()
    }

    private fun stripTags(text: String): String =
        text.replace(Regex("<[^>]*>"), " ").replace(Regex("""\s+"""), " ").trim()

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

            // "MW con servicio" no es un déficit, así que no sirve de respaldo si falta "MW afectados"
            val deficitMw = item.mwAffected

            val nowMs = item.publishedAtMs ?: System.currentTimeMillis()
            val dateLabel = (if (item.publishedAtMs == null) "Ingresado " else "") + formatDate(nowMs)

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

    internal fun resolveCircuit(name: String): List<CircuitInfo> {
        val norm = normalize(cleanFeedName(name))
        if (norm.isBlank()) return emptyList()
        nameIndex[norm]?.let { return it }
        return listOfNotNull(aliasIndex[norm])
    }

    /**
     * Quita solo los comentarios entre paréntesis ("Niquero (completo)" -> "Niquero").
     * Los códigos como "o813" se conservan: distinguen circuitos distintos ("Las Mercedes o813" / "o809").
     */
    internal fun cleanFeedName(name: String): String =
        name.trim().replace(Regex("""\s*\([^)]*\)"""), "").trim()

    companion object {
        fun normalize(s: String): String =
            java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replace(Regex("""\p{Mn}+"""), "")
                .lowercase(Locale.ROOT)
                .replace(Regex("""\s+"""), " ")

        /**
         * Zona de las horas del feed que no traen offset ("2026-11-01T20:03:07").
         * Se comprobó que NO son hora de Cuba (UTC-5): el feed publicó avisos con una hora posterior a la hora
         * local de La Habana en ese momento. Se asumen UTC; si resultara ser otra zona, basta cambiar esta constante.
         */
        val FEED_NAIVE_TIME_ZONE: TimeZone = TimeZone.getTimeZone("UTC")

        private val FORMATS_WITH_ZONE = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "EEE, dd MMM yyyy HH:mm:ss Z"
        )
        private val FORMATS_WITHOUT_ZONE = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )

        /** Fecha del feed en ms. Un offset explícito manda; sin offset se usa [FEED_NAIVE_TIME_ZONE]. */
        fun parseFeedDate(value: String?): Long? {
            if (value.isNullOrBlank()) return null
            val text = value.trim()
            return FORMATS_WITH_ZONE.firstNotNullOfOrNull { parseExact(it, text, null) }
                ?: FORMATS_WITHOUT_ZONE.firstNotNullOfOrNull { parseExact(it, text, FEED_NAIVE_TIME_ZONE) }
        }

        /** Interpreta el texto completo (no solo un prefijo, que dejaría ignorar un offset) con el formato dado. */
        private fun parseExact(pattern: String, text: String, zone: TimeZone?): Long? {
            val format = SimpleDateFormat(pattern, Locale.US)
            if (zone != null) format.timeZone = zone
            val position = ParsePosition(0)
            val date = format.parse(text, position)
            return if (date != null && position.index == text.length) date.time else null
        }
    }

    private fun formatDate(ms: Long): String =
        SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale("es")).format(Date(ms))
}

/** Item del feed parseado, usado internamente para construir ElectricalReport. */
data class FeedItem(
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
package com.example.data.parser

import java.text.SimpleDateFormat
import java.util.Locale

/** Una publicación del canal: identificador ("canal/123"), texto limpio y hora de publicación. */
data class ChannelPost(val id: String?, val text: String, val publishedAtMs: Long?)

/** Extrae las publicaciones de la vista pública de un canal (https://t.me/s/<canal>). No depende de Android. */
object TelegramChannelParser {

    private val MESSAGE_TEXT = Regex(
        """<div class="tgme_widget_message_text[^"]*js-message_text[^"]*"[^>]*>(.*?)</div>""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val DATA_POST = Regex("""data-post="([^"]+)"""")
    private val TIME_ATTR = Regex("""<time[^>]*datetime="([^"]+)"""")

    /** true si el HTML parece la vista pública de un canal de Telegram. */
    fun looksLikeChannelView(html: String) = html.contains("tgme_widget_message")

    /** Publicaciones en el orden de la página (de la más antigua a la más reciente). Las que no tienen texto se omiten. */
    fun parse(html: String): List<ChannelPost> =
        html.split("tgme_widget_message_wrap").drop(1).mapNotNull { chunk ->
            // Se usa js-message_text para no confundir el texto citado de una respuesta con el del mensaje
            val body = MESSAGE_TEXT.find(chunk)?.groupValues?.get(1) ?: return@mapNotNull null
            val text = htmlToText(body)
            if (text.isBlank()) null
            else ChannelPost(
                id = DATA_POST.find(chunk)?.groupValues?.get(1),
                text = text,
                publishedAtMs = TIME_ATTR.find(chunk)?.groupValues?.get(1)?.let { parseIso(it) }
            )
        }

    fun htmlToText(fragment: String): String =
        decodeEntities(
            fragment.replace(Regex("""<br\s*/?>"""), "\n").replace(Regex("<[^>]*>"), "")
        ).lines().joinToString("\n") { it.trim() }.trim()

    /** Texto plano de una página cualquiera (solo para fuentes que no son Telegram). */
    fun plainText(html: String): String =
        decodeEntities(
            html.replace(Regex("<script[\\s\\S]*?</script>"), " ")
                .replace(Regex("<style[\\s\\S]*?</style>"), " ")
                .replace(Regex("<[^>]*>"), " ")
        ).replace(Regex("\\s+"), " ").trim()

    private fun decodeEntities(s: String): String =
        s.replace(Regex("""&#x([0-9a-fA-F]+);""")) { codePoint(it.groupValues[1].toInt(16)) }
            .replace(Regex("""&#(\d+);""")) { codePoint(it.groupValues[1].toInt()) }
            .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")

    private fun codePoint(cp: Int): String =
        if (Character.isValidCodePoint(cp)) String(Character.toChars(cp)) else ""

    private fun parseIso(value: String): Long? = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(value)?.time
    } catch (e: Exception) {
        null
    }
}

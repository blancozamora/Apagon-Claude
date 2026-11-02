package com.example.data.parser

import com.example.data.model.BlockUpdate
import com.example.data.model.CircuitInfo
import com.example.data.model.CircuitStatus
import com.example.data.model.CircuitUpdate
import com.example.data.model.ElectricalReport
import com.example.data.model.STATUS_TTL_MS
import com.example.data.provider.GranmaDataProvider
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Interpreta el texto de un comunicado y lo traduce a cambios de estado concretos.
 *
 * Reglas principales (todas conservadoras: ante la duda NO se modifica ningún circuito):
 *  - Un número de circuito ("circuito 3") solo se resuelve si se sabe de qué municipio habla
 *    (mencionado en la misma frase o en las inmediatamente anteriores).
 *  - El estado se toma de la cláusula donde aparece el circuito/bloque, no de toda la nota.
 *  - Una mención de bloque sin palabra de estado ("Bloque 2") no cambia nada.
 *  - Las frases de pronóstico ("se prevé…", "estimada…") no cambian estados.
 *
 * No depende de Android, por lo que se puede probar con tests unitarios simples.
 */
class ReportTextParser(private val catalog: List<CircuitInfo> = GranmaDataProvider.getDefaultCircuits()) {

    private class Mention(val position: Int, val municipality: String)
    private class StatusMark(val start: Int, val end: Int, val status: CircuitStatus)

    // ---------- Índices del catálogo ----------

    /** municipio (nombre original) -> expresión regular con él y sus alias, ya normalizados */
    private val municipalityRegex: Map<String, Regex> = catalog.map { it.municipality }.distinct().associateWith { muni ->
        val aliases = (listOf(normalize(muni)) + (MUNICIPALITY_ALIASES[muni] ?: emptyList())).distinct()
        Regex("""\b(?:${aliases.joinToString("|") { Regex.escape(it) }})\b""")
    }

    private val municipalityWords: Set<String> = catalog.map { it.municipality }.distinct()
        .flatMap { listOf(normalize(it)) + (MUNICIPALITY_ALIASES[it] ?: emptyList()) }.toSet()

    private val plainNumbered = Regex("""^circuito \d+$""")

    /** Circuitos con nombre propio: frase completa ("circuito niquero 1") y, si existe, rasgo distintivo ("hospital"). */
    private val fullNameRegex: Map<CircuitInfo, Regex> = catalog.mapNotNull { c ->
        val n = normalize(c.name)
        if (plainNumbered.matches(n)) null else c to Regex("""\b${Regex.escape(n)}\b""")
    }.toMap()

    private val distinctiveToken: Map<CircuitInfo, String> = catalog.mapNotNull { c ->
        var n = " " + normalize(c.name).replace(Regex("""\d+"""), " ") + " "
        n = n.replace(" circuito ", " ")
        municipalityWords.sortedByDescending { it.length }.forEach { n = n.replace(" $it ", " ") }
        val token = n.trim().replace(Regex("""\s+"""), " ")
        if (token.length >= 4) c to token else null
    }.toMap()

    private val circuitsByToken: Map<String, List<CircuitInfo>> =
        distinctiveToken.entries.groupBy({ it.value }, { it.key })

    private val tokenRegex: Map<String, Regex> =
        circuitsByToken.keys.associateWith { Regex("""\b${Regex.escape(it)}\b""") }

    // ---------- API pública ----------

    fun parse(
        text: String,
        sourceUrl: String = "Texto ingresado",
        publishedAtMs: Long? = null,
        postId: String? = null
    ): ElectricalReport {
        val raw = stripTags(text)
        val norm = normalize(raw)

        val deficit = extractDeficitMw(norm)

        val circuitStatus = LinkedHashMap<String, CircuitStatus>()   // id -> estado (el último mencionado gana)
        val blockStatus = LinkedHashMap<Int, CircuitStatus>()
        val unresolved = LinkedHashSet<String>()

        var carried: String? = null
        var carriedTtl = 0

        for (sentence in splitSentences(norm)) {
            val mentions = findMunicipalities(sentence)
            val forecast = FORECAST.containsMatchIn(sentence)

            if (!forecast) {
                for (clause in splitClauses(sentence)) {
                    val clauseMentions = findMunicipalities(clause)
                    val ctx = if (clauseMentions.isNotEmpty()) clauseMentions.last().municipality else carried
                    val marks = statusMarks(clause)
                    if (marks.isNotEmpty()) {
                        resolveCircuitRefs(clause, clauseMentions, ctx, marks, circuitStatus, unresolved)
                        resolveBlockRefs(clause, marks, blockStatus)
                    }
                    if (clauseMentions.isNotEmpty()) {
                        carried = clauseMentions.last().municipality
                        carriedTtl = CONTEXT_CLAUSES
                    } else if (carriedTtl > 0 && --carriedTtl == 0) {
                        carried = null
                    }
                }
            } else if (mentions.isNotEmpty()) {
                carried = mentions.last().municipality
                carriedTtl = CONTEXT_CLAUSES
            }
        }

        val byId = catalog.associateBy { it.id }
        val circuitUpdates = circuitStatus.map { (id, st) -> CircuitUpdate(id, st) }
        val blockUpdates = blockStatus.map { (n, st) -> BlockUpdate("Bloque $n", st) }

        fun label(id: String) = byId[id]?.let { "${it.municipality} – ${it.name}" } ?: id
        val outage = circuitUpdates.filter { it.status == CircuitStatus.OUTAGE }.map { label(it.circuitId) }
        val energized = circuitUpdates.filter { it.status == CircuitStatus.ENERGIZED }.map { label(it.circuitId) }
        val blocksOut = blockUpdates.filter { it.status == CircuitStatus.OUTAGE }.map { it.block }

        val summary = buildString {
            append(if (deficit != null) "Déficit de generación detectado: $deficit MW. " else "Comunicado procesado. ")
            if (blocksOut.isNotEmpty()) append("Bloques afectados: ${blocksOut.joinToString(", ")}. ")
            if (outage.isNotEmpty()) append("Sin servicio: ${outage.joinToString(", ")}. ")
            if (energized.isNotEmpty()) append("Con servicio: ${energized.joinToString(", ")}. ")
            if (unresolved.isNotEmpty()) append("No se pudo asignar municipio a: ${unresolved.joinToString(", ")}. ")
            if (circuitUpdates.isEmpty() && blockUpdates.isEmpty() && unresolved.isEmpty()) {
                append("No se identificaron circuitos ni bloques con cambio de estado.")
            }
        }.trim()

        val nowMs = System.currentTimeMillis()
        val effectiveTs = publishedAtMs ?: nowMs
        val dateLabel = (if (publishedAtMs == null) "Ingresado " else "") + formatDate(effectiveTs)

        return ElectricalReport(
            id = UUID.randomUUID().toString(),
            date = dateLabel,
            title = if (norm.contains("parte") || norm.contains("nota")) "Parte Informativo Procesado" else "Actualización del Canal Oficial",
            rawText = raw,
            sourceUrl = sourceUrl,
            generationDeficitMW = deficit,
            affectedBlocks = blocksOut,
            energizedCircuits = energized,
            outageCircuits = outage,
            aiSummary = summary,
            timestampMs = effectiveTs,
            postId = postId,
            circuitUpdates = circuitUpdates,
            blockUpdates = blockUpdates
        )
    }

    /**
     * Convierte las publicaciones de un canal en comunicados, de la más antigua a la más reciente.
     * Solo se consideran las publicadas dentro de [windowMs] y que traigan información útil;
     * si no hay ninguna, se devuelve el análisis de la última publicación para que el usuario vea el resultado.
     */
    fun reportsFromPosts(
        posts: List<ChannelPost>,
        sourceUrl: String,
        nowMs: Long = System.currentTimeMillis(),
        windowMs: Long = STATUS_TTL_MS
    ): List<ElectricalReport> {
        if (posts.isEmpty()) return emptyList()
        val sorted = posts.sortedBy { it.publishedAtMs ?: Long.MAX_VALUE }
        val inWindow = sorted.filter { it.publishedAtMs == null || nowMs - it.publishedAtMs <= windowMs }
        val parsed = inWindow.map { parse(it.text, sourceUrl, it.publishedAtMs, it.id) }
        val relevant = parsed.filter { it.hasContent }
        return relevant.ifEmpty { listOf(parse(sorted.last().text, sourceUrl, sorted.last().publishedAtMs, sorted.last().id)) }
    }

    // ---------- Segmentación ----------

    private fun splitSentences(norm: String): List<String> =
        norm.replace(Regex("""\bn[o°º]\.\s*(?=\d)"""), "no ")
            .split('\n')
            .flatMap { it.split(Regex("""(?<=[.;!?])\s+""")) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /**
     * Divide una frase en cláusulas por comas y " y ", pero solo cierra una cláusula cuando ya
     * contiene una palabra de estado. Así "circuitos 1, 3 y 5 afectados" queda como una sola cláusula,
     * y "circuito 1 afectado y circuito 2 restablecido" como dos.
     */
    private fun splitClauses(sentence: String): List<String> {
        val pieces = sentence.split(Regex("""\s*,\s*|\s+y\s+"""))
        val clauses = mutableListOf<String>()
        var acc = StringBuilder()
        for (piece in pieces) {
            if (acc.isNotEmpty()) acc.append(", ")
            acc.append(piece)
            if (statusMarks(acc.toString()).isNotEmpty()) {
                clauses += acc.toString()
                acc = StringBuilder()
            }
        }
        if (acc.isNotEmpty()) clauses += acc.toString()
        return clauses
    }

    // ---------- Resolución de referencias ----------

    private fun resolveCircuitRefs(
        clause: String,
        mentions: List<Mention>,
        ctx: String?,
        marks: List<StatusMark>,
        out: MutableMap<String, CircuitStatus>,
        unresolved: MutableSet<String>
    ) {
        // 1) "circuito(s) N, M y K": requiere municipio
        for (m in CIRCUIT_NUMBERS.findAll(clause)) {
            val numbers = parseNumberList(m.groupValues[1])
            val muni = municipalityFor(m.range.first, mentions, ctx)
            val status = statusFor(m.range.first, m.range.last + 1, marks) ?: continue
            if (muni == null) {
                unresolved += "Circuito ${numbers.sorted().joinToString("/")}"
                continue
            }
            for (n in numbers) {
                val matches = catalog.filter { it.municipality == muni && numberOf(it) == n }
                if (matches.isEmpty()) unresolved += "$muni – Circuito $n" else matches.forEach { out[it.id] = status }
            }
        }

        // 2) Nombre completo del circuito ("circuito niquero 1", "circuito cabo cruz")
        for ((circuit, regex) in fullNameRegex) {
            val m = regex.find(clause) ?: continue
            val status = statusFor(m.range.first, m.range.last + 1, marks) ?: continue
            out[circuit.id] = status
        }

        // 3) Rasgo distintivo ("hospital", "aeropuerto"…); si es ambiguo, se desambigua por municipio
        for ((token, regex) in tokenRegex) {
            val m = regex.find(clause) ?: continue
            val status = statusFor(m.range.first, m.range.last + 1, marks) ?: continue
            var candidates = circuitsByToken.getValue(token)
            if (candidates.size > 1) {
                val muni = municipalityFor(m.range.first, mentions, ctx)
                candidates = if (muni != null) candidates.filter { it.municipality == muni } else emptyList()
                if (candidates.size != 1) {
                    unresolved += "Circuito $token"
                    continue
                }
            }
            // Un rasgo suelto solo cuenta si el circuito tiene nombre propio (no basta con nombrar el municipio)
            out[candidates.first().id] = status
        }
    }

    private fun resolveBlockRefs(clause: String, marks: List<StatusMark>, out: MutableMap<Int, CircuitStatus>) {
        for (m in BLOCK_NUMBERS.findAll(clause)) {
            val status = statusFor(m.range.first, m.range.last + 1, marks) ?: continue
            parseNumberList(m.groupValues[1]).filter { it in 1..4 }.forEach { out[it] = status }
        }
    }

    private fun findMunicipalities(text: String): List<Mention> =
        municipalityRegex.flatMap { (muni, regex) -> regex.findAll(text).map { Mention(it.range.first, muni) }.toList() }
            .sortedBy { it.position }

    private fun municipalityFor(pos: Int, mentions: List<Mention>, carried: String?): String? {
        val before = mentions.filter { it.position <= pos }.maxByOrNull { it.position }
        if (before != null) return before.municipality
        val after = mentions.filter { it.position > pos }.minByOrNull { it.position }
        return after?.municipality ?: carried
    }

    private fun numberOf(c: CircuitInfo): Int? = Regex("""\d+""").find(c.name)?.value?.toIntOrNull()

    // ---------- Estados ----------

    private fun statusMarks(text: String): List<StatusMark> {
        val accepted = mutableListOf<StatusMark>()
        for ((regex, status) in STATUS_PATTERNS) {
            for (m in regex.findAll(text)) {
                val a = m.range.first
                val b = m.range.last + 1
                if (accepted.none { it.start < b && a < it.end }) accepted += StatusMark(a, b, status)
            }
        }
        return accepted.sortedBy { it.start }
    }

    /** Estado de la marca más cercana a la referencia [start, end); si dos estados distintos empatan, no se decide. */
    private fun statusFor(start: Int, end: Int, marks: List<StatusMark>): CircuitStatus? {
        fun gap(m: StatusMark) = when {
            m.start >= end -> m.start - end
            m.end <= start -> start - m.end
            else -> 0
        }
        val best = marks.minOf { gap(it) }
        val winners = marks.filter { gap(it) == best }.map { it.status }.toSet()
        return winners.singleOrNull()
    }

    // ---------- Utilidades ----------

    private fun parseNumberList(group: String): Set<Int> {
        val result = linkedSetOf<Int>()
        var rest = group
        for (r in Regex("""(\d+)\s*(?:-|al)\s*(\d+)""").findAll(group)) {
            val a = r.groupValues[1].toInt()
            val b = r.groupValues[2].toInt()
            if (b >= a && b - a <= 30) (a..b).forEach { result += it }
        }
        rest = rest.replace(Regex("""(\d+)\s*(?:-|al)\s*(\d+)"""), " ")
        Regex("""\d+""").findAll(rest).forEach { it.value.toIntOrNull()?.let { n -> result += n } }
        return result
    }

    private fun extractDeficitMw(norm: String): Int? {
        // 1) Busca valores con unidad MW cerca de palabras de déficit
        for (m in MW_REGEX.findAll(norm)) {
            val start = m.range.first
            val end = m.range.last
            val beforeWindow = norm.substring(maxOf(0, start - 60), start)
            val afterWindow = norm.substring(end, minOf(norm.length, end + 60))
            if (!DEFICIT_WORDS.containsMatchIn(beforeWindow) && !DEFICIT_WORDS.containsMatchIn(afterWindow)) continue
            val value = parseMw(m.groupValues[1]) ?: continue
            return value
        }

        // 2) Fallback: busca números cerca de palabras de déficit sin unidad MW explícita
        //    Ejemplo: "afectados 96 con servicio 16"
        val deficitMatches = DEFICIT_WORDS.findAll(norm)
        for (dm in deficitMatches) {
            val pos = dm.range.first
            // Busca número en ventana de ±40 chars alrededor de la palabra de déficit
            val windowStart = maxOf(0, pos - 40)
            val windowEnd = minOf(norm.length, pos + 40)
            val window = norm.substring(windowStart, windowEnd)
            val numberMatch = Regex("""(\d{1,3}(?:[.,]\d{3})*|\d+)""").find(window)
            if (numberMatch != null) {
                val value = parseMw(numberMatch.groupValues[1])
                if (value != null && value > 0) return value
            }
        }

        return null
    }

    private fun parseMw(raw: String): Int? {
        val s = raw.replace(" ", "")
        return when {
            Regex("""\d{1,3}([.,]\d{3})+""").matches(s) -> s.replace(Regex("""[.,]"""), "").toIntOrNull()
            s.contains('.') || s.contains(',') -> s.replace(',', '.').toDoubleOrNull()?.let { Math.round(it).toInt() }
            else -> s.toIntOrNull()
        }
    }

    private fun stripTags(text: String): String = text.replace(Regex("<[^>]*>"), " ").trim()

    private fun formatDate(ms: Long): String =
        SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale("es")).format(Date(ms))

    companion object {
        private const val CONTEXT_CLAUSES = 6

        fun normalize(s: String): String =
            Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("""\p{Mn}+"""), "").lowercase(Locale.ROOT)

        private val MUNICIPALITY_ALIASES = mapOf("Bartolomé Masó" to listOf("maso"))

        private val NUM_LIST = """\d+(?:\s*(?:,|y|e|/|-|al)\s*(?:no\s*)?\d+)*"""
        private val CIRCUIT_NUMBERS = Regex("""\bcircuitos?\s*(?:no\s*)?($NUM_LIST)""")
        private val BLOCK_NUMBERS = Regex("""\bbloques?\s*(?:no\s*)?($NUM_LIST)""")

        private val FORECAST = Regex("""\b(?:se\s+preve\w*|previst\w*|pronostic\w*|se\s+espera\w*|estimad\w*|posible\w*)\b""")

        private val MW_REGEX = Regex("""(\d[\d., ]*\d|\d)\s*(?:mw|megavatios?)\b""")
        private val DEFICIT_WORDS = Regex("""deficit|deficiencia|afectaci""")

        /** El orden importa: las negaciones ("sin afectación") van primero para ganar a "afectación". */
        private val STATUS_PATTERNS: List<Pair<Regex, CircuitStatus>> = listOf(
            Regex("""\b(?:sin|no\s+hay|no\s+tiene|no\s+tienen)\s+afectaci(?:on|ones)\b""") to CircuitStatus.ENERGIZED,
            Regex("""\bno\s+afectad[oa]s?\b""") to CircuitStatus.ENERGIZED,
            Regex("""\bsin\s+(?:servicio|luz|corriente|energia|fluido)\b""") to CircuitStatus.OUTAGE,
            Regex("""\bfuera\s+de\s+servicio\b""") to CircuitStatus.OUTAGE,
            Regex("""\bdesconectad[oa]s?\b""") to CircuitStatus.OUTAGE,
            Regex("""\bapagad[oa]s?\b""") to CircuitStatus.OUTAGE,
            Regex("""\bapagon(?:es)?\b""") to CircuitStatus.OUTAGE,
            Regex("""\binterrumpid[oa]s?\b""") to CircuitStatus.OUTAGE,
            Regex("""\brestablec\w*""") to CircuitStatus.ENERGIZED,
            Regex("""\benergizad[oa]s?\b""") to CircuitStatus.ENERGIZED,
            Regex("""\bcon\s+(?:servicio|luz|corriente|energia|fluido)\b""") to CircuitStatus.ENERGIZED,
            Regex("""\ben\s+servicio\b""") to CircuitStatus.ENERGIZED,
            Regex("""\brecuperad[oa]s?\b""") to CircuitStatus.ENERGIZED,
            Regex("""\bnormalizad[oa]s?\b""") to CircuitStatus.ENERGIZED,
            Regex("""\bprotegid[oa]s?\b""") to CircuitStatus.ENERGIZED,
            Regex("""\bafectad[oa]s?\b""") to CircuitStatus.OUTAGE,
            Regex("""\bafectaci(?:on|ones)\b""") to CircuitStatus.OUTAGE
        )
    }
}

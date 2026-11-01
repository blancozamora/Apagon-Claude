package com.example.data

import com.example.data.model.CircuitInfo
import com.example.data.model.CircuitStatus
import com.example.data.model.STATUS_TTL_MS
import com.example.data.parser.ReportTextParser
import com.example.data.parser.TelegramChannelParser
import com.example.data.provider.GranmaDataProvider
import com.example.data.repository.GridUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportParsingTest {

    private val parser = ReportTextParser()
    private val now = 1_800_000_000_000L

    /** Aplica un texto al catálogo y devuelve id -> estado de los circuitos con estado conocido. */
    private fun statusAfter(text: String, publishedAt: Long = now): Map<String, CircuitStatus> {
        val report = parser.parse(text, publishedAtMs = publishedAt)
        return GridUpdater.applyReport(GranmaDataProvider.getDefaultCircuits(), report, now)
            .filter { it.status != CircuitStatus.UNKNOWN }.associate { it.id to it.status }
    }

    @Test
    fun `numero de circuito solo afecta al municipio indicado`() {
        val s = statusAfter("Bayamo: circuitos 1, 3 y 5 afectados.")
        assertEquals(setOf("bay_1", "bay_3", "bay_5"), s.keys)
        assertTrue(s.values.all { it == CircuitStatus.OUTAGE })
    }

    @Test
    fun `varios municipios y estados en el mismo parte`() {
        val s = statusAfter(
            """PARTE INFORMATIVO
            |Bayamo: circuitos 1, 3 y 5 afectados.
            |Circuito 2 restablecido.
            |Manzanillo: circuito 1 afectado; circuito 3 Hospital protegido.""".trimMargin()
        )
        assertEquals(CircuitStatus.OUTAGE, s["bay_1"])
        assertEquals(CircuitStatus.ENERGIZED, s["bay_2"])
        assertEquals(CircuitStatus.OUTAGE, s["bay_3"])
        assertEquals(CircuitStatus.OUTAGE, s["bay_5"])
        assertEquals(CircuitStatus.OUTAGE, s["mnz_1"])
        assertEquals(CircuitStatus.ENERGIZED, s["mnz_3"])
        assertNull(s["bay_6"])      // el hospital de Bayamo no se menciona
        assertNull(s["niq_1"])      // otros "Circuito 1" no se tocan
        assertNull(s["jig_1"])
    }

    @Test
    fun `circuito con nombre propio ambiguo se resuelve por municipio`() {
        val s = statusAfter("Manzanillo: circuito Hospital sin servicio.")
        assertEquals(mapOf("mnz_3" to CircuitStatus.OUTAGE), s)
    }

    @Test
    fun `numero sin municipio no cambia nada`() {
        val report = parser.parse("Circuito 1 afectado.", publishedAtMs = now)
        assertTrue(report.circuitUpdates.isEmpty())
        assertTrue(report.aiSummary.contains("No se pudo asignar municipio"))
    }

    @Test
    fun `circuito de otro municipio indicado con de`() {
        val s = statusAfter("Circuito 1 de Niquero afectado.")
        assertEquals(mapOf("niq_1" to CircuitStatus.OUTAGE), s)
    }

    @Test
    fun `bloque restablecido no se marca como apagon`() {
        val report = parser.parse("Bloque 2 restablecido, bloques 1 y 3 afectados.", publishedAtMs = now)
        val byBlock = report.blockUpdates.associate { it.block to it.status }
        assertEquals(CircuitStatus.ENERGIZED, byBlock["Bloque 2"])
        assertEquals(CircuitStatus.OUTAGE, byBlock["Bloque 1"])
        assertEquals(CircuitStatus.OUTAGE, byBlock["Bloque 3"])
        assertEquals(listOf("Bloque 1", "Bloque 3"), report.affectedBlocks)
    }

    @Test
    fun `mencion de bloque sin estado no cambia nada`() {
        assertTrue(parser.parse("Se informa sobre el Bloque 2.", publishedAtMs = now).blockUpdates.isEmpty())
    }

    @Test
    fun `las previsiones no cambian estados`() {
        val report = parser.parse("Se prevé afectación en los bloques 1 y 2 mañana.", publishedAtMs = now)
        assertTrue(report.blockUpdates.isEmpty())
    }

    @Test
    fun `circuito de prioridad y notas no se pisan por un bloque`() {
        val s = statusAfter("Bloque 1 afectado.")
        assertNull(s["bay_6"])  // Prioritario
        assertEquals(CircuitStatus.OUTAGE, s["bay_1"])
        val circuits = GridUpdater.applyReport(
            GranmaDataProvider.getDefaultCircuits(), parser.parse("Circuito 6 de Bayamo afectado.", publishedAtMs = now), now
        )
        assertEquals("Hospital Provincial Carlos Manuel de Céspedes", circuits.first { it.id == "bay_6" }.notes)
    }

    @Test
    fun `el circuito explicito gana sobre el bloque`() {
        val s = statusAfter("Bloque 1 afectado. Bayamo: circuito 1 restablecido.")
        assertEquals(CircuitStatus.ENERGIZED, s["bay_1"])
        assertEquals(CircuitStatus.OUTAGE, s["bay_3"])  // también Bloque 1
    }

    @Test
    fun `deficit en MW con separador de miles y solo si se llama deficit`() {
        assertEquals(1200, parser.parse("Déficit de 1 200 MW en el día.").generationDeficitMW)
        assertEquals(75, parser.parse("Afectación de 75 MW.").generationDeficitMW)
        assertNull(parser.parse("Disponibilidad de 900 MW.").generationDeficitMW)
    }

    @Test
    fun `un comunicado viejo no pisa uno nuevo ni se aplica si esta vencido`() {
        val newer = parser.parse("Bayamo: circuito 1 restablecido.", publishedAtMs = now - 1_000)
        val older = parser.parse("Bayamo: circuito 1 afectado.", publishedAtMs = now - 60_000)
        var c = GridUpdater.applyReport(GranmaDataProvider.getDefaultCircuits(), newer, now)
        c = GridUpdater.applyReport(c, older, now)
        assertEquals(CircuitStatus.ENERGIZED, c.first { it.id == "bay_1" }.status)

        val expired = parser.parse("Bayamo: circuito 1 afectado.", publishedAtMs = now - STATUS_TTL_MS - 1)
        val untouched = GridUpdater.applyReport(GranmaDataProvider.getDefaultCircuits(), expired, now)
        assertEquals(CircuitStatus.UNKNOWN, untouched.first { it.id == "bay_1" }.status)
    }

    @Test
    fun `los estados viejos vuelven a desconocido`() {
        val report = parser.parse("Bayamo: circuito 1 afectado.", publishedAtMs = now)
        val applied = GridUpdater.applyReport(GranmaDataProvider.getDefaultCircuits(), report, now)
        val later = GridUpdater.expireStale(applied, now + STATUS_TTL_MS + 1)
        assertEquals(CircuitStatus.UNKNOWN, later.first { it.id == "bay_1" }.status)
        assertTrue(later.first { it.id == "bay_1" }.lastUpdate.startsWith("Desactualizado"))
        assertEquals(CircuitStatus.OUTAGE, GridUpdater.expireStale(applied, now + 1000).first { it.id == "bay_1" }.status)
    }

    @Test
    fun `canal de Telegram con respuesta citada, hora y entidades`() {
        val html = """
            <div class="tgme_widget_message_wrap js-widget_message_wrap">
              <div class="tgme_widget_message" data-post="granma_empresa/101">
                <a class="tgme_widget_message_reply"><div class="tgme_widget_message_text js-message_reply_text">Texto citado</div></a>
                <div class="tgme_widget_message_text js-message_text" dir="auto">Bayamo: circuito 1 afectado<br/>Circuito 2 &amp; 3 restablecidos</div>
                <a class="tgme_widget_message_date"><time datetime="2027-01-15T12:00:00+00:00" class="time">12:00</time></a>
              </div>
            </div>
            <div class="tgme_widget_message_wrap js-widget_message_wrap">
              <div class="tgme_widget_message" data-post="granma_empresa/102"><div class="tgme_widget_message_photo"></div></div>
            </div>"""
        val posts = TelegramChannelParser.parse(html)
        assertEquals(1, posts.size)
        assertEquals("granma_empresa/101", posts[0].id)
        assertEquals("Bayamo: circuito 1 afectado\nCircuito 2 & 3 restablecidos", posts[0].text)
        assertEquals(1_800_014_400_000L, posts[0].publishedAtMs)
    }

    @Test
    fun `reportsFromPosts repite los mensajes recientes en orden y descarta los viejos`() {
        val html = { id: Int, iso: String, text: String ->
            """<div class="tgme_widget_message_wrap"><div data-post="c/$id"><div class="tgme_widget_message_text js-message_text">$text</div><time datetime="$iso"></time></div></div>"""
        }
        val posts = TelegramChannelParser.parse(
            html(1, "2027-01-15T01:00:00+00:00", "Bayamo: circuito 1 afectado.") +
            html(2, "2027-01-15T09:00:00+00:00", "Bayamo: circuito 1 restablecido.") +
            html(3, "2027-01-15T09:30:00+00:00", "Felicitaciones a todos.")
        )
        val at = 1_800_014_400_000L // 2027-01-15T12:00Z
        val reports = parser.reportsFromPosts(posts, "t.me/s/c", nowMs = at, windowMs = 4 * 3600_000L)
        assertEquals(listOf("c/2"), reports.map { it.postId })      // el 1 está fuera de ventana; el 3 no tiene datos
        assertEquals(CircuitStatus.ENERGIZED, reports[0].circuitUpdates.single().status)
    }
}

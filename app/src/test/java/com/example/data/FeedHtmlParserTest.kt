package com.example.data

import com.example.data.model.CircuitInfo
import com.example.data.model.CircuitStatus
import com.example.data.parser.FeedHtmlParser
import com.example.data.parser.FeedItem
import com.example.data.provider.GranmaDataProvider
import com.example.data.repository.GridUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedHtmlParserTest {

    private val parser = FeedHtmlParser()
    private val catalog = GranmaDataProvider.getDefaultCircuits()

    private companion object {
        /** 2026-11-01T20:03:07Z en milisegundos */
        const val UTC_20_03_07 = 1793563387000L
    }

    @Test
    fun `parsea feed HTML real de tg-i-c-a con múltiples artículos`() {
        val html = feedHtmlSample()
        val items = parser.parse(html)

        assertEquals(10, items.size)

        // parse() devuelve de la más antigua a la más reciente
        assertEquals("41508", items.first().postId)
        val first = items.last()
        assertEquals("[Photo] ⚡️Actualización del Sistema Electroenergético en la Provincia de Granma.⚡️", first.title)
        assertEquals("41518", first.postId)
        assertNotNull(first.publishedAtMs)
        assertEquals(58, first.mwAffected)
        assertEquals(35, first.mwService)
        assertEquals(6, first.outageNames.size)
        assertEquals(3, first.energizedNames.size)
        assertEquals(5, first.rotationNames.size)

        assertTrue(first.outageNames.contains("Charco Redondo"))
        assertTrue(first.outageNames.contains("Palmarón"))
        assertTrue(first.outageNames.contains("El Yarey"))
        assertTrue(first.outageNames.contains("Santa Rita"))
        assertTrue(first.outageNames.contains("Horacio Rodríguez"))
        assertTrue(first.outageNames.contains("Saco Centro"))

        assertTrue(first.energizedNames.contains("Desarrollo"))
        assertTrue(first.energizedNames.contains("Jiguaní Desarrollo"))
        assertTrue(first.energizedNames.contains("Banda Ecil"))

        assertTrue(first.rotationNames.contains("Jimmy Hirzel"))
        assertTrue(first.rotationNames.contains("Siboney Convertido"))
        assertTrue(first.rotationNames.contains("Guisa"))
        assertTrue(first.rotationNames.contains("Julia"))
        assertTrue(first.rotationNames.contains("San Ramón"))
    }

    @Test
    fun `convierte items a ElectricalReport con circuitUpdates resueltos`() {
        val html = feedHtmlSample()
        val items = parser.parse(html)
        val reports = parser.toReports(items, sourceUrl = "https://tg.i-c-a.su/rss/UNE_EEG")

        assertEquals(10, reports.size)

        // De la más antigua a la más reciente: la última es el aviso de las 20:03
        val latest = reports.last()
        assertEquals("41518", latest.postId)
        assertEquals(58, latest.generationDeficitMW)

        fun ids(status: CircuitStatus) =
            latest.circuitUpdates.filter { it.status == status }.map { it.circuitId }.toSet()

        assertEquals(
            setOf("jig_3", "feed_palmaron", "feed_el_yarey", "feed_santa_rita", "feed_horacio_rodriguez", "feed_saco_centro"),
            ids(CircuitStatus.OUTAGE)
        )
        assertEquals(
            setOf("feed_desarrollo", "feed_jiguani_desarrollo", "feed_banda_ecil"),
            ids(CircuitStatus.ENERGIZED)
        )
        assertEquals(
            setOf("feed_jimmy_hirzel", "feed_siboney_convertido", "gui_1", "feed_julia", "cam_2"),
            ids(CircuitStatus.ROTATION)
        )
        // Los 14 nombres del aviso se resolvieron
        assertEquals(14, latest.circuitUpdates.size)
        assertTrue(!latest.aiSummary.contains("No resueltos"))
    }

    @Test
    fun `todos los nombres del feed resuelven salvo Las Mercedes sin codigo`() {
        val items = parser.parse(feedHtmlSample())
        val names = items.flatMap { it.outageNames + it.energizedNames + it.rotationNames }.toSet()
        val unresolved = names.filter { parser.resolveCircuit(it).isEmpty() }
        // "Las Mercedes" aparece una vez sin código (o813/o809): es ambiguo y no se adivina
        assertEquals(listOf("Las Mercedes"), unresolved)
    }

    @Test
    fun `resuelve nombres de circuito del feed contra catálogo`() {
        fun ids(name: String) = parser.resolveCircuit(name).map { it.id }

        // Equivalencias explícitas con el catálogo base (GranmaDataProvider.getFeedAliases)
        assertEquals(listOf("jig_3"), ids("Charco Redondo"))
        assertEquals(listOf("pil_2"), ids("Marea del Portillo"))
        assertEquals(listOf("yar_2"), ids("Sofía"))
        assertEquals(listOf("niq_1"), ids("Niquero (completo)"))
        assertEquals(listOf("med_1"), ids("Media Luna"))
        assertEquals(listOf("cau_1"), ids("Cauto Cristo"))
        assertEquals(listOf("gui_1"), ids("Guisa"))

        // Circuitos propios del feed (nombre exacto)
        assertEquals(listOf("feed_hospital_infantil"), ids("Hospital Infantil"))
        assertEquals(listOf("feed_guisa_2"), ids("Guisa 2"))
        assertEquals(listOf("feed_jiguani"), ids("Jiguaní"))
        assertEquals(listOf("feed_desarrollo"), ids("Desarrollo"))
        assertEquals(listOf("feed_jiguani_desarrollo"), ids("Jiguaní Desarrollo"))

        // Los códigos distinguen circuitos distintos; mayúsculas y acentos no importan
        assertEquals(listOf("feed_las_mercedes_o813"), ids("Las Mercedes o813"))
        assertEquals(listOf("feed_las_mercedes_o809"), ids("Las Mercedes o809"))
        assertEquals(listOf("feed_ceramica_roja_o275"), ids("CERAMICA ROJA o275"))

        // Sin código es ambiguo y un nombre desconocido no se adivina por parecido
        assertTrue(ids("Las Mercedes").isEmpty())
        assertTrue(ids("Circuito Inventado").isEmpty())
    }

    @Test
    fun `el catalogo completo no repite ids ni nombres`() {
        val all = GranmaDataProvider.getDefaultCircuits()
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertEquals(all.size, all.map { FeedHtmlParser.normalize(it.municipality + "|" + it.name) }.toSet().size)
        // Las equivalencias apuntan a circuitos que existen
        val ids = all.map { it.id }.toSet()
        assertTrue(GranmaDataProvider.getFeedAliases().values.all { it in ids })
    }

    @Test
    fun `quita los parentesis del nombre pero conserva los codigos`() {
        assertEquals("Niquero", parser.cleanFeedName("Niquero (completo)"))
        assertEquals("Las Mercedes o813", parser.cleanFeedName("Las Mercedes o813"))
        assertEquals("Las Mercedes o809", parser.cleanFeedName("Las Mercedes o809"))
        assertEquals("Charco Redondo", parser.cleanFeedName("Charco Redondo"))
    }

    @Test
    fun `fecha sin zona se lee como UTC sin depender de la zona del dispositivo`() {
        // 2026-11-01T20:03:07Z
        assertEquals(UTC_20_03_07, FeedHtmlParser.parseFeedDate("2026-11-01T20:03:07"))
        assertEquals(UTC_20_03_07, FeedHtmlParser.parseFeedDate("2026-11-01 20:03:07"))
    }

    @Test
    fun `un offset explicito manda sobre la zona asumida`() {
        assertEquals(UTC_20_03_07, FeedHtmlParser.parseFeedDate("2026-11-01T20:03:07Z"))
        assertEquals(UTC_20_03_07, FeedHtmlParser.parseFeedDate("2026-11-01T20:03:07+00:00"))
        assertEquals(UTC_20_03_07, FeedHtmlParser.parseFeedDate("2026-11-01T15:03:07-05:00"))
        assertEquals(UTC_20_03_07, FeedHtmlParser.parseFeedDate("2026-11-01T20:03:07.000Z"))
        assertEquals(UTC_20_03_07, FeedHtmlParser.parseFeedDate("Sun, 01 Nov 2026 20:03:07 +0000"))
    }

    @Test
    fun `fechas invalidas devuelven null`() {
        assertEquals(null, FeedHtmlParser.parseFeedDate(null))
        assertEquals(null, FeedHtmlParser.parseFeedDate(""))
        assertEquals(null, FeedHtmlParser.parseFeedDate("ayer"))
        assertEquals(null, FeedHtmlParser.parseFeedDate("2026-11-01"))
        assertEquals(null, FeedHtmlParser.parseFeedDate("2026-11-01T20:03:07 basura"))
    }

    @Test
    fun `aplicar todo el feed en orden deja el estado del ultimo aviso y descarta lo vencido`() {
        val reports = parser.toReports(parser.parse(feedHtmlSample()), sourceUrl = "feed")
        // 7 minutos después del último aviso (20:03:07 UTC). Se fija con una constante y no con parseFeedDate,
        // para que esta prueba no se anule a sí misma si la zona asumida estuviera mal.
        val now = UTC_20_03_07 + 7 * 60 * 1000L

        var circuits = GranmaDataProvider.getDefaultCircuits()
        reports.forEach { circuits = GridUpdater.applyReport(circuits, it, now) }
        fun status(id: String) = circuits.first { it.id == id }.status

        assertEquals(CircuitStatus.OUTAGE, status("feed_palmaron"))             // 20:03 afectado
        assertEquals(CircuitStatus.ENERGIZED, status("feed_desarrollo"))        // 17:26 afectado, 20:03 restablecido
        assertEquals(CircuitStatus.ROTATION, status("feed_jimmy_hirzel"))       // 20:03 mayor tiempo de afectación
        assertEquals(CircuitStatus.OUTAGE, status("feed_campechuela_pueblo"))   // 17:26 afectado y no se vuelve a nombrar
        // Solo aparece a las 14:06, que ya tiene más de 6 h: vuelve a no tener datos.
        // (Esta comprobación depende de leer las horas del feed como UTC.)
        assertEquals(CircuitStatus.UNKNOWN, status("feed_diesel_caymare"))
        // Un circuito que el feed nunca menciona sigue sin datos
        assertEquals(CircuitStatus.UNKNOWN, status("bay_1"))
    }

    private fun feedHtmlSample(): String = """
        <!DOCTYPE html>
        <html lang="es">
        <head>
          <meta charset="UTF-8">
          <title>Empresa Eléctrica de Granma 👷🇨🇺 — Feed</title>
        </head>
        <body>
          <div class="wrap">
            <header class="feed">
              <h1>Empresa Eléctrica de Granma 👷🇨🇺</h1>
            </header>
            <main>
              <article class="item">
                <h2>[Photo] ⚡️Actualización del Sistema Electroenergético en la Provincia de Granma.⚡️</h2>
                <div class="item-meta">
                  <time datetime="2026-11-01T20:03:07">2026-11-01 20:03:07</time> ·
                  <a href="https://t.me/une_eeg/41518">https://t.me/une_eeg/41518</a>
                </div>
                <p><strong>⚡️Actualización del Sistema Electroenergético en la Provincia de Granma.⚡️</strong></p>
                <ul>
                  <li><strong>MW afectados:</strong> 58</li>
                  <li><strong>MW con servicio:</strong> 35</li>
                </ul>
                <p class="bad">❌ Se afecta el servicio eléctrico para los circuitos:</p>
                <ul>
                  <li>Charco Redondo</li>
                  <li>Palmarón</li>
                  <li>El Yarey</li>
                  <li>Santa Rita</li>
                  <li>Horacio Rodríguez</li>
                  <li>Saco Centro</li>
                </ul>
                <p class="ok">✅ Se restablece el servicio eléctrico para los circuitos:</p>
                <ul>
                  <li>Desarrollo</li>
                  <li>Jiguaní Desarrollo</li>
                  <li>Banda Ecil</li>
                </ul>
                <p class="warn">⏱️ Circuitos con mayor tiempo de afectación:</p>
                <ul>
                  <li>Jimmy Hirzel</li>
                  <li>Siboney Convertido</li>
                  <li>Guisa</li>
                  <li>Julia</li>
                  <li>San Ramón</li>
                </ul>
                <p>📢 <strong>Continuaremos informando</strong>.</p>
              </article>
              <article class="item">
                <h2>[Photo] ⚡️Actualización del Sistema Electroenergético en la Provincia de Granma.⚡️</h2>
                <div class="item-meta">
                  <time datetime="2026-11-01T19:27:13">2026-11-01 19:27:13</time> ·
                  <a href="https://t.me/une_eeg/41517">https://t.me/une_eeg/41517</a>
                </div>
                <ul>
                  <li><strong>MW afectados:</strong> 59</li>
                  <li><strong>MW con servicio:</strong> 25</li>
                </ul>
                <p class="bad">❌ Se afecta el servicio eléctrico para los circuitos:</p>
                <ul>
                  <li>Guisa 2</li>
                  <li>Granma</li>
                  <li>Santa Isabel</li>
                  <li>Aguadita</li>
                  <li>Pilón</li>
                  <li>Calicito</li>
                  <li>Ceiba Hueca</li>
                </ul>
                <p class="ok">✅ Se restablece el servicio eléctrico para los circuitos:</p>
                <ul>
                  <li>General Benítez</li>
                  <li>Veguita Sur</li>
                  <li>Las Novillas</li>
                  <li>Cerámica Roja O275</li>
                  <li>San Francisco</li>
                </ul>
                <p class="warn">⏱️ Circuitos con mayor tiempo de afectación:</p>
                <ul>
                  <li>Marea del Portillo</li>
                  <li>Jimmy Hirzel</li>
                  <li>Jiguaní Desarrollo</li>
                  <li>Siboney Convertido</li>
                  <li>Guisa</li>
                </ul>
              </article>
              <article class="item">
                <h2>[Photo] ⚡️Actualización del Sistema Electroenergético en la Provincia de Granma.⚡️</h2>
                <div class="item-meta">
                  <time datetime="2026-11-01T18:19:47">2026-11-01 18:19:47</time> ·
                  <a href="https://t.me/une_eeg/41516">https://t.me/une_eeg/41516</a>
                </div>
                <ul>
                  <li><strong>MW afectados:</strong> 57</li>
                  <li><strong>MW con servicio:</strong> 56</li>
                </ul>
                <p class="bad">❌ Se afecta el servicio eléctrico para el circuito:</p>
                <ul>
                  <li>Bueycito</li>
                </ul>
                <p class="ok">✅ Se restablece el servicio eléctrico para los circuitos:</p>
                <ul>
                  <li>La Curva</li>
                  <li>Cayo Redondo</li>
                  <li>Cayama</li>
                  <li>Desarrollo Cayama</li>
                </ul>
                <p class="warn">⏱️ Circuitos con mayor tiempo de afectación:</p>
                <ul>
                  <li>Veguita Sur</li>
                  <li>Marea del Portillo</li>
                  <li>Las Novillas</li>
                  <li>Cerámica Roja O275</li>
                  <li>San Francisco</li>
                </ul>
              </article>
              <article class="item">
                <h2>[Photo] ⚡️Actualización del Sistema Electroenergético en la Provincia de Granma.⚡️</h2>
                <div class="item-meta">
                  <time datetime="2026-11-01T17:26:44">2026-11-01 17:26:44</time> ·
                  <a href="https://t.me/une_eeg/41514">https://t.me/une_eeg/41514</a>
                </div>
                <ul>
                  <li><strong>MW afectados:</strong> 59</li>
                  <li><strong>MW con servicio:</strong> 43</li>
                </ul>
                <p class="bad">❌ Se afecta el servicio eléctrico para los circuitos:</p>
                <ul>
                  <li>Desarrollo</li>
                  <li>Campechuela Pueblo</li>
                </ul>
                <p class="ok">✅ Se restablece el servicio eléctrico para los circuitos:</p>
                <ul>
                  <li>Hospital Infantil</li>
                  <li>Cauto Cristo</li>
                  <li>Ocho Preceba</li>
                  <li>Hospital Infantil</li>
                  <li>Saco Centro</li>
                </ul>
                <p class="warn">⏱️ Circuitos con mayor tiempo de afectación:</p>
                <ul>
                  <li>Marea del Portillo</li>
                  <li>Veguita Sur</li>
                  <li>Las Novillas</li>
                  <li>Cerámica Roja O275</li>
                  <li>Cayama</li>
                </ul>
              </article>
              <article class="item">
                <h2>[Photo] ⚡️Actualización del Sistema Electroenergético en la Provincia de Granma.⚡️</h2>
                <div class="item-meta">
                  <time datetime="2026-11-01T16:32:54">2026-11-01 16:32:54</time> ·
                  <a href="https://t.me/une_eeg/41513">https://t.me/une_eeg/41513</a>
                </div>
                <ul>
                  <li><strong>MW afectados:</strong> 64</li>
                  <li><strong>MW con servicio:</strong> 50</li>
                </ul>
                <p class="bad">❌ Se afecta el servicio eléctrico para los circuitos:</p>
                <ul>
                  <li>La Sal</li>
                  <li>Niquero (completo)</li>
                  <li>Mabay</li>
                  <li>Bartolomé Masó</li>
                  <li>Las Mercedes o813</li>
                  <li>Yara Pueblo</li>
                  <li>Sofía</li>
                  <li>Fábrica de Conservas</li>
                </ul>
                <p class="ok">✅ Se restablece el servicio eléctrico para los circuitos:</p>
                <ul>
                  <li>Calicito</li>
                  <li>Providencia</li>
                  <li>Charco Redondo</li>
                  <li>Palmarón</li>
                  <li>El Yarey</li>
                  <li>Las Mercedes o809</li>
                  <li>Ceiba Hueca</li>
                </ul>
                <p class="warn">⏱️ Circuitos con mayor tiempo de afectación:</p>
                <ul>
                  <li>Calicito</li>
                  <li>Ocho Preceba</li>
                  <li>Marea del Portillo</li>
                  <li>Veguita Sur</li>
                  <li>Hospital Infantil</li>
                </ul>
              </article>
              <article class="item">
                <h2>[Photo] ⚡️Actualización del Sistema Electroenergético en la Provincia de Granma.⚡️</h2>
                <div class="item-meta">
                  <time datetime="2026-11-01T16:04:32">2026-11-01 16:04:32</time> ·
                  <a href="https://t.me/une_eeg/41512">https://t.me/une_eeg/41512</a>
                </div>
                <ul>
                  <li><strong>MW afectados:</strong> 58</li>
                  <li><strong>MW con servicio:</strong> 51</li>
                </ul>
                <p class="bad">❌ Se afecta el servicio eléctrico para el circuito:</p>
                <ul>
                  <li>Nuevo Yao</li>
                </ul>
                <p class="ok">✅ Se restablece el servicio eléctrico para los circuitos:</p>
                <ul>
                  <li>Guisa 2</li>
                  <li>Granma</li>
                  <li>Pilón</li>
                  <li>Aguadita</li>
                  <li>Santa Isabel</li>
                  <li>Bueycito</li>
                  <li>Jiguaní</li>
                </ul>
                <p class="warn">⏱️ Circuitos con mayor tiempo de afectación:</p>
                <ul>
                  <li>Calicito</li>
                  <li>Cauto Cristo</li>
                  <li>Ocho Preceba</li>
                  <li>Las Mercedes</li>
                  <li>Providencia</li>
                </ul>
              </article>
              <article class="item">
                <h2>[Photo] ⚡️Actualización del Sistema Electroenergético en la Provincia de Granma.⚡️</h2>
                <div class="item-meta">
                  <time datetime="2026-11-01T15:27:09">2026-11-01 15:27:09</time> ·
                  <a href="https://t.me/une_eeg/41511">https://t.me/une_eeg/41511</a>
                </div>
                <ul>
                  <li><strong>MW afectados:</strong> 63</li>
                  <li><strong>MW con servicio:</strong> 46</li>
                </ul>
                <p class="bad">❌ Se afecta el servicio eléctrico para los circuitos:</p>
                <ul>
                  <li>Guisa</li>
                  <li>Julia</li>
                  <li>San Ramón</li>
                </ul>
                <p class="ok">✅ Se restablece el servicio eléctrico para los circuitos:</p>
                <ul>
                  <li>Campechuela Pueblo</li>
                  <li>Santa Rita</li>
                  <li>Doce y Medio</li>
                  <li>Horacio Rodríguez</li>
                </ul>
                <p class="warn">⏱️ Circuitos con mayor tiempo de afectación:</p>
                <ul>
                  <li>Aguadita</li>
                  <li>Pilón</li>
                  <li>Jiguaní</li>
                  <li>Bueycito</li>
                  <li>Calicito</li>
                </ul>
              </article>
              <article class="item">
                <h2>[Photo] ⚡️Actualización del Sistema Electroenergético en la Provincia de Granma.⚡️</h2>
                <div class="item-meta">
                  <time datetime="2026-11-01T14:06:56">2026-11-01 14:06:56</time> ·
                  <a href="https://t.me/une_eeg/41510">https://t.me/une_eeg/41510</a>
                </div>
                <ul>
                  <li><strong>MW afectados:</strong> 55</li>
                  <li><strong>MW con servicio:</strong> 37</li>
                </ul>
                <p class="ok">✅ Se restablece el servicio eléctrico para los circuitos:</p>
                <ul>
                  <li>Diésel Caymare</li>
                  <li>Yara Pueblo</li>
                  <li>Sofía</li>
                  <li>Fábrica de Conservas</li>
                  <li>Mabay</li>
                  <li>Niquero (completo)</li>
                  <li>Media Luna</li>
                  <li>La Jagua</li>
                </ul>
                <p class="warn">⏱️ Circuitos con mayor tiempo de afectación:</p>
                <ul>
                  <li>Campechuela Pueblo</li>
                  <li>Aguadita</li>
                  <li>Pilón</li>
                  <li>Jiguaní</li>
                  <li>Bueycito</li>
                </ul>
              </article>
              <article class="item">
                <h2>[Photo] 🚨 Comunicamos a los clientes que pertenecen al circuito Montes Verdes que el mismo se [...]</h2>
                <div class="item-meta">
                  <time datetime="2026-11-01T13:37:49">2026-11-01 13:37:49</time> ·
                  <a href="https://t.me/une_eeg/41509">https://t.me/une_eeg/41509</a>
                </div>
                <p>🚨 Comunicamos a los clientes que pertenecen al circuito <strong>Montes Verdes</strong> que el mismo se encuentra disparado.</p>
              </article>
              <article class="item">
                <h2>‼️Situación del SEN para el 01 de noviembre de 2026.‼️</h2>
                <div class="item-meta">
                  <time datetime="2026-11-01T13:23:47">2026-11-01 13:23:47</time> ·
                  <a href="https://t.me/une_eeg/41508">https://t.me/une_eeg/41508</a>
                </div>
                <p>👉🏽 En el día de ayer se afectó el servicio por déficit de capacidad durante las 24 horas...</p>
              </article>
            </main>
          </div>
        </body>
        </html>
    """.trimIndent()
}
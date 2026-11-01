package com.example.data

import com.example.data.model.CircuitInfo
import com.example.data.model.CircuitStatus
import com.example.data.parser.FeedHtmlParser
import com.example.data.provider.GranmaDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedHtmlParserTest {

    private val parser = FeedHtmlParser()
    private val catalog = GranmaDataProvider.getDefaultCircuits()

    @Test
    fun `parsea feed HTML real de tg.i-c-a.su con múltiples artículos`() {
        val html = feedHtmlSample()
        val items = parser.parse(html)

        assertEquals(10, items.size)

        val first = items[0]
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

        val firstReport = reports[0]
        assertEquals("41518", firstReport.postId)
        assertEquals(58, firstReport.generationDeficitMW)
        assertTrue(firstReport.circuitUpdates.isNotEmpty())

        val outageIds = firstReport.circuitUpdates.filter { it.status == CircuitStatus.OUTAGE }.map { it.circuitId }
        val energizedIds = firstReport.circuitUpdates.filter { it.status == CircuitStatus.ENERGIZED }.map { it.circuitId }
        val rotationIds = firstReport.circuitUpdates.filter { it.status == CircuitStatus.ROTATION }.map { it.circuitId }

        assertTrue(outageIds.contains("jig_3"))  // Charco Redondo
        assertTrue(energizedIds.any { it == "bay_1" || it == "bay_2" || it == "bay_3" || it == "bay_4" || it == "bay_5" || it == "bay_6" || it == "bay_7" })
        assertTrue(rotationIds.isNotEmpty())
    }

    @Test
    fun `resuelve nombres de circuito del feed contra catálogo`() {
        // Charco Redondo -> jig_3 (Circuito Charco Redondo)
        val matches = parser.resolveCircuit("Charco Redondo")
        assertEquals(1, matches.size)
        assertEquals("jig_3", matches[0].id)

        // Marea del Portillo -> pil_2
        val matches2 = parser.resolveCircuit("Marea del Portillo")
        assertEquals(1, matches2.size)
        assertEquals("pil_2", matches2[0].id)

        // Sofía -> yar_2
        val matches3 = parser.resolveCircuit("Sofía")
        assertEquals(1, matches3.size)
        assertEquals("yar_2", matches3[0].id)

        // Hospital Infantil -> bay_6 (Circuito 6 Hospital)
        val matches4 = parser.resolveCircuit("Hospital Infantil")
        assertTrue(matches4.any { it.id == "bay_6" })

        // Niquero (completo) -> niq_1 o niq_2
        val matches5 = parser.resolveCircuit("Niquero (completo)")
        assertTrue(matches5.any { it.id.startsWith("niq_") })

        // Media Luna -> med_1
        val matches6 = parser.resolveCircuit("Media Luna")
        assertTrue(matches6.any { it.id == "med_1" })

        // Cauto Cristo -> cau_1
        val matches7 = parser.resolveCircuit("Cauto Cristo")
        assertEquals(1, matches7.size)
        assertEquals("cau_1", matches7[0].id)
    }

    @Test
    fun `limpia sufijos del nombre del feed`() {
        assertEquals("Niquero", parser.cleanFeedName("Niquero (completo)"))
        assertEquals("Las Mercedes", parser.cleanFeedName("Las Mercedes o813"))
        assertEquals("Las Mercedes", parser.cleanFeedName("Las Mercedes o809"))
        assertEquals("Charco Redondo", parser.cleanFeedName("Charco Redondo"))
    }

    @Test
    fun `parsea fecha ISO sin zona horaria`() {
        val ms = FeedHtmlParser.parseFeedDate("2026-11-01T20:03:07")
        assertNotNull(ms)
        // Verificar que corresponde a 2026-11-01 20:03:07
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
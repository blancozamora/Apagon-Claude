package com.example.data.provider

import com.example.data.model.CircuitInfo
import com.example.data.model.CircuitStatus

object GranmaDataProvider {

    /**
     * Catálogo completo que usa la app: el catálogo base más los circuitos tal como los nombra el feed oficial.
     */
    fun getDefaultCircuits(): List<CircuitInfo> = getBaseCircuits() + getFeedCircuits()

    /**
     * Catálogo base de infraestructura eléctrica de la Provincia de Granma
     * (Municipios, subestaciones y bloques asignados). Es el que entiende el parser de texto libre
     * ([com.example.data.parser.ReportTextParser]): circuitos numerados por municipio y bloques.
     *
     * IMPORTANTE: Todos los circuitos inician con estado UNKNOWN (Desconocido / Sin datos)
     * y sin fechas ni notas simuladas, a la espera de los comunicados oficiales reales.
     */
    fun getBaseCircuits(): List<CircuitInfo> {
        return listOf(
            // BAYAMO
            CircuitInfo("bay_1", "Circuito 1", "Bayamo", "Subestación Bayamo 110kV", "Bloque 1", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("bay_2", "Circuito 2", "Bayamo", "Subestación Bayamo 110kV", "Bloque 2", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("bay_3", "Circuito 3", "Bayamo", "Subestación Siboney 33kV", "Bloque 1", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("bay_4", "Circuito 4", "Bayamo", "Subestación Siboney 33kV", "Bloque 3", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("bay_5", "Circuito 5", "Bayamo", "Subestación Bayamo 110kV", "Bloque 2", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("bay_6", "Circuito 6 Hospital", "Bayamo", "Subestación Bayamo 110kV", "Prioritario", CircuitStatus.UNKNOWN, "Sin datos", notes = "Hospital Provincial Carlos Manuel de Céspedes"),
            CircuitInfo("bay_7", "Circuito Aeropuerto", "Bayamo", "Subestación Aeropuerto", "Bloque 3", CircuitStatus.UNKNOWN, "Sin datos"),

            // MANZANILLO
            CircuitInfo("mnz_1", "Circuito 1", "Manzanillo", "Subestación Manzanillo 110kV", "Bloque 1", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("mnz_2", "Circuito 2", "Manzanillo", "Subestación Manzanillo 110kV", "Bloque 2", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("mnz_3", "Circuito 3 Hospital", "Manzanillo", "Subestación Manzanillo 110kV", "Prioritario", CircuitStatus.UNKNOWN, "Sin datos", notes = "Hospital Celia Sánchez Manduley"),
            CircuitInfo("mnz_4", "Circuito 4", "Manzanillo", "Subestación Blanquizal", "Bloque 3", CircuitStatus.UNKNOWN, "Sin datos"),

            // JIGUANÍ
            CircuitInfo("jig_1", "Circuito Jiguaní 1", "Jiguaní", "Subestación Jiguaní 33kV", "Bloque 1", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("jig_2", "Circuito Jiguaní 2", "Jiguaní", "Subestación Jiguaní 33kV", "Bloque 2", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("jig_3", "Circuito Charco Redondo", "Jiguaní", "Subestación Charco Redondo", "Bloque 3", CircuitStatus.UNKNOWN, "Sin datos"),

            // NIQUERO
            CircuitInfo("niq_1", "Circuito Niquero 1", "Niquero", "Subestación Niquero 33kV", "Bloque 2", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("niq_2", "Circuito Cabo Cruz", "Niquero", "Subestación Niquero", "Bloque 1", CircuitStatus.UNKNOWN, "Sin datos"),

            // CAMPECHUELA
            CircuitInfo("cam_1", "Circuito Campechuela 1", "Campechuela", "Subestación Campechuela", "Bloque 3", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("cam_2", "Circuito San Ramón", "Campechuela", "Subestación San Ramón", "Bloque 1", CircuitStatus.UNKNOWN, "Sin datos"),

            // GUISA
            CircuitInfo("gui_1", "Circuito Guisa 1", "Guisa", "Subestación Guisa 33kV", "Bloque 2", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("gui_2", "Circuito Monjará", "Guisa", "Subestación Guisa", "Bloque 1", CircuitStatus.UNKNOWN, "Sin datos"),

            // MEDIA LUNA
            CircuitInfo("med_1", "Circuito Media Luna 1", "Media Luna", "Subestación Media Luna", "Bloque 1", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("med_2", "Circuito Vituco", "Media Luna", "Subestación Media Luna", "Bloque 3", CircuitStatus.UNKNOWN, "Sin datos"),

            // PILÓN
            CircuitInfo("pil_1", "Circuito Pilón 1", "Pilón", "Subestación Pilón", "Bloque 2", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("pil_2", "Circuito Marea del Portillo", "Pilón", "Subestación Marea", "Bloque 1", CircuitStatus.UNKNOWN, "Sin datos"),

            // BARTOLOMÉ MASÓ
            CircuitInfo("mas_1", "Circuito Masó 1", "Bartolomé Masó", "Subestación Masó 33kV", "Bloque 1", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("mas_2", "Circuito Caney de las Mercedes", "Bartolomé Masó", "Subestación Caney", "Bloque 3", CircuitStatus.UNKNOWN, "Sin datos"),

            // YARA
            CircuitInfo("yar_1", "Circuito Yara 1", "Yara", "Subestación Yara 33kV", "Bloque 2", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("yar_2", "Circuito Sofía", "Yara", "Subestación Sofía", "Bloque 1", CircuitStatus.UNKNOWN, "Sin datos"),

            // RÍO CAUTO
            CircuitInfo("rio_1", "Circuito Río Cauto 1", "Río Cauto", "Subestación Río Cauto", "Bloque 1", CircuitStatus.UNKNOWN, "Sin datos"),
            CircuitInfo("rio_2", "Circuito Vado del Yeso", "Río Cauto", "Subestación Vado", "Bloque 3", CircuitStatus.UNKNOWN, "Sin datos"),

            // BUEY ARRIBA
            CircuitInfo("bue_1", "Circuito Buey Arriba 1", "Buey Arriba", "Subestación Buey Arriba", "Bloque 2", CircuitStatus.UNKNOWN, "Sin datos"),

            // CAUTO CRISTO
            CircuitInfo("cau_1", "Circuito Cauto Cristo 1", "Cauto Cristo", "Subestación Cauto Cristo", "Bloque 1", CircuitStatus.UNKNOWN, "Sin datos")
        )
    }

    /**
     * Circuitos con el nombre EXACTO con que aparecen en el feed de UNE_EEG (nombre, municipio) y que no
     * tienen equivalente en el catálogo base (ver [getFeedAliases] para los que sí).
     *
     * - El feed no informa municipio, bloque ni subestación: el municipio es PRELIMINAR y debe verificarse;
     *   el bloque queda "Sin bloque" (así ningún "Bloque N" los toca) y la subestación "Sin datos".
     * - Los códigos forman parte del nombre ("Las Mercedes o813" y "Las Mercedes o809" son circuitos distintos).
     */
    private val FEED_CIRCUITS: List<Pair<String, String>> = listOf(
        // Bayamo
        "Palmarón" to "Bayamo", "El Yarey" to "Bayamo", "Santa Rita" to "Bayamo",
        "Horacio Rodríguez" to "Bayamo", "Saco Centro" to "Bayamo", "Desarrollo" to "Bayamo",
        "Banda Ecil" to "Bayamo", "Jimmy Hirzel" to "Bayamo", "Siboney Convertido" to "Bayamo",
        "Granma" to "Bayamo", "Hospital Infantil" to "Bayamo", "Ocho Preceba" to "Bayamo",
        "Nuevo Yao" to "Bayamo", "Doce y Medio" to "Bayamo", "Montes Verdes" to "Bayamo",
        // Jiguaní
        "Jiguaní" to "Jiguaní", "Jiguaní Desarrollo" to "Jiguaní",
        // Guisa
        "Julia" to "Guisa", "Guisa 2" to "Guisa",
        // Campechuela
        "Santa Isabel" to "Campechuela", "Calicito" to "Campechuela", "Ceiba Hueca" to "Campechuela",
        "Campechuela Pueblo" to "Campechuela",
        // Manzanillo
        "Aguadita" to "Manzanillo",
        // Cauto Cristo
        "General Benítez" to "Cauto Cristo",
        // Río Cauto
        "Veguita Sur" to "Río Cauto", "Las Novillas" to "Río Cauto", "Cerámica Roja O275" to "Río Cauto",
        "San Francisco" to "Río Cauto", "La Curva" to "Río Cauto", "Cayo Redondo" to "Río Cauto",
        "Cayama" to "Río Cauto", "Desarrollo Cayama" to "Río Cauto",
        // Buey Arriba
        "Bueycito" to "Buey Arriba",
        // Niquero
        "La Sal" to "Niquero", "Mabay" to "Niquero",
        // Bartolomé Masó
        "Bartolomé Masó" to "Bartolomé Masó", "Providencia" to "Bartolomé Masó",
        // Yara
        "Las Mercedes o813" to "Yara", "Las Mercedes o809" to "Yara", "Yara Pueblo" to "Yara",
        "Fábrica de Conservas" to "Yara",
        // Media Luna
        "Diésel Caymare" to "Media Luna", "La Jagua" to "Media Luna"
    )

    fun getFeedCircuits(): List<CircuitInfo> = FEED_CIRCUITS.map { (name, municipality) ->
        CircuitInfo(
            id = "feed_" + slug(name),
            name = name,
            municipality = municipality,
            substation = "Sin datos",
            block = "Sin bloque",
            status = CircuitStatus.UNKNOWN,
            lastUpdate = "Sin datos"
        )
    }

    /**
     * Nombres del feed que corresponden a un circuito del catálogo base (nombre del feed -> id).
     * Son los emparejamientos que antes se hacían por coincidencia parcial de texto; ahora son explícitos.
     * Si alguno es incorrecto, se corrige aquí.
     */
    fun getFeedAliases(): Map<String, String> = mapOf(
        "Charco Redondo" to "jig_3",
        "San Ramón" to "cam_2",
        "Sofía" to "yar_2",
        "Marea del Portillo" to "pil_2",
        "Pilón" to "pil_1",
        "Guisa" to "gui_1",
        "Cauto Cristo" to "cau_1",
        "Media Luna" to "med_1",
        "Niquero (completo)" to "niq_1"
    )

    private fun slug(name: String): String =
        java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
            .lowercase(java.util.Locale.ROOT)
            .replace(Regex("""[^a-z0-9]+"""), "_")
            .trim('_')
}

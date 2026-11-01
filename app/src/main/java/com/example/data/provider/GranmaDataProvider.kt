package com.example.data.provider

import com.example.data.model.CircuitInfo
import com.example.data.model.CircuitStatus

object GranmaDataProvider {

    /**
     * Catálogo oficial de infraestructura eléctrica de la Provincia de Granma
     * (Municipios, subestaciones y bloques asignados).
     *
     * IMPORTANTE: Todos los circuitos inician con estado UNKNOWN (Desconocido / Sin datos)
     * y sin fechas ni notas simuladas, a la espera de los comunicados oficiales reales.
     */
    fun getDefaultCircuits(): List<CircuitInfo> {
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
}

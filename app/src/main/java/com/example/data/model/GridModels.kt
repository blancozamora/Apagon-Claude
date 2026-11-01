package com.example.data.model

/**
 * Tiempo máximo (6 h) que un estado se considera vigente. Pasado ese plazo sin un
 * comunicado nuevo, el circuito vuelve a UNKNOWN para no mostrar datos viejos como actuales.
 */
const val STATUS_TTL_MS: Long = 6L * 60 * 60 * 1000

enum class CircuitStatus {
    ENERGIZED,    // Con servicio / Energizado (Green)
    OUTAGE,       // Sin servicio / Apagado (Red)
    ROTATION,     // En afectación por rotación (Orange)
    UNKNOWN       // Desconocido / En verificación (Gray)
}

data class CircuitInfo(
    val id: String,
    val name: String,             // Ej. "Circuito 1", "Circuito Niquero"
    val municipality: String,     // Ej. "Bayamo", "Manzanillo"
    val substation: String,       // Ej. "Subestación Bayamo 110kV"
    val block: String,            // Ej. "Bloque 1", "Bloque 2"
    val status: CircuitStatus,
    val lastUpdate: String,       // Ej. "Hoy, 08:30 AM"
    val estimatedRestoration: String? = null, // Ej. "14:00 hrs"
    val isFavorite: Boolean = false,
    val notes: String = "",
    val updatedAtMs: Long = 0L    // Momento (ms) del comunicado que fijó el estado actual; 0 = nunca actualizado
)

data class MunicipalityStatus(
    val name: String,
    val totalCircuits: Int,
    val energizedCount: Int,
    val outageCount: Int,
    val rotationCount: Int,
    val statusSummary: String
) {
    val percentageEnergized: Int
        get() = if (totalCircuits > 0) (energizedCount * 100) / totalCircuits else 0
}

/** Cambio de estado de un circuito concreto, ya resuelto contra el catálogo (municipio + número/nombre). */
data class CircuitUpdate(val circuitId: String, val status: CircuitStatus)

/** Cambio de estado de un bloque completo (ej. "Bloque 2"). */
data class BlockUpdate(val block: String, val status: CircuitStatus)

data class ElectricalReport(
    val id: String,
    val date: String,             // Ej. "01/11/2026 - 08:00 AM"
    val title: String,            // Ej. "Parte Informativo de la Empresa Eléctrica Granma"
    val rawText: String,          // Full official announcement text
    val sourceUrl: String,        // Ej. "https://t.me/s/une_eeg"
    val generationDeficitMW: Int? = null, // Ej. 75 MW
    val affectedBlocks: List<String> = emptyList(), // Ej. ["Bloque 1", "Bloque 3"]
    val energizedCircuits: List<String> = emptyList(),
    val outageCircuits: List<String> = emptyList(),
    val aiSummary: String = "",
    val timestampMs: Long = System.currentTimeMillis(), // Hora de publicación del mensaje (o de ingreso manual)
    val postId: String? = null,                          // Ej. "granma_empresa/1234"; sirve para no procesar dos veces el mismo mensaje
    val circuitUpdates: List<CircuitUpdate> = emptyList(),
    val blockUpdates: List<BlockUpdate> = emptyList()
) {
    /** true si el comunicado trae información útil (estados, bloques o déficit). */
    val hasContent: Boolean
        get() = circuitUpdates.isNotEmpty() || blockUpdates.isNotEmpty() || generationDeficitMW != null
}

data class CommunityReport(
    val id: String,
    val userAlias: String,
    val municipality: String,
    val circuitName: String,
    val status: CircuitStatus,
    val timestamp: String,
    val comment: String
)

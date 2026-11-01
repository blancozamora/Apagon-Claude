package com.example.data.repository

import com.example.data.model.CircuitInfo
import com.example.data.model.CircuitStatus
import com.example.data.model.ElectricalReport
import com.example.data.model.STATUS_TTL_MS

/** Lógica pura (sin estado ni Android) para aplicar comunicados al catálogo de circuitos. */
object GridUpdater {

    /**
     * Aplica los cambios de un comunicado. Un circuito con actualización explícita gana sobre la de su bloque,
     * un comunicado más viejo nunca pisa uno más nuevo, y los comunicados ya vencidos se ignoran.
     * No se tocan las notas del circuito.
     */
    fun applyReport(circuits: List<CircuitInfo>, report: ElectricalReport, nowMs: Long): List<CircuitInfo> {
        if (report.circuitUpdates.isEmpty() && report.blockUpdates.isEmpty()) return circuits
        val ts = report.timestampMs
        if (nowMs - ts > STATUS_TTL_MS) return circuits

        val byCircuit = report.circuitUpdates.associate { it.circuitId to it.status }
        val byBlock = report.blockUpdates.associate { it.block to it.status }

        return circuits.map { c ->
            val newStatus = byCircuit[c.id] ?: byBlock[c.block] ?: return@map c
            if (ts < c.updatedAtMs) c
            else c.copy(status = newStatus, lastUpdate = report.date, updatedAtMs = ts)
        }
    }

    /** Devuelve a UNKNOWN los circuitos cuyo último dato supera el tiempo de vigencia. */
    fun expireStale(circuits: List<CircuitInfo>, nowMs: Long): List<CircuitInfo> =
        circuits.map { c ->
            if (c.status != CircuitStatus.UNKNOWN && c.updatedAtMs > 0 && nowMs - c.updatedAtMs > STATUS_TTL_MS) {
                c.copy(status = CircuitStatus.UNKNOWN, lastUpdate = "Desactualizado (${c.lastUpdate})")
            } else c
        }
}

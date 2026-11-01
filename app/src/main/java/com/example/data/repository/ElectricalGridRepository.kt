package com.example.data.repository

import com.example.data.model.CircuitInfo
import com.example.data.model.CircuitStatus
import com.example.data.model.CommunityReport
import com.example.data.model.ElectricalReport
import com.example.data.model.MunicipalityStatus
import com.example.data.parser.ReportParserEngine
import com.example.data.provider.GranmaDataProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class ElectricalGridRepository {

    private val parserEngine = ReportParserEngine()

    private val _circuits = MutableStateFlow<List<CircuitInfo>>(GranmaDataProvider.getDefaultCircuits())
    val circuits: StateFlow<List<CircuitInfo>> = _circuits.asStateFlow()

    private val _reports = MutableStateFlow<List<ElectricalReport>>(emptyList())
    val reports: StateFlow<List<ElectricalReport>> = _reports.asStateFlow()

    private val _communityReports = MutableStateFlow<List<CommunityReport>>(emptyList())
    val communityReports: StateFlow<List<CommunityReport>> = _communityReports.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val processedPostIds = mutableSetOf<String>()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastScrapedUrl = MutableStateFlow("https://api.rss2json.com/v1/api.json?rss_url=https://tg.i-c-a.su/rss/UNE_EEG")
    val lastScrapedUrl: StateFlow<String> = _lastScrapedUrl.asStateFlow()

    fun getMunicipalityStatuses(): List<MunicipalityStatus> {
        val allCircuits = _circuits.value
        val grouped = allCircuits.groupBy { it.municipality }

        return grouped.map { (muniName, mCircuits) ->
            val energized = mCircuits.count { it.status == CircuitStatus.ENERGIZED }
            val outage = mCircuits.count { it.status == CircuitStatus.OUTAGE }
            val rotation = mCircuits.count { it.status == CircuitStatus.ROTATION }
            val unknown = mCircuits.count { it.status == CircuitStatus.UNKNOWN }

            val statusText = when {
                unknown == mCircuits.size -> "Pendiente de reporte"
                outage > energized -> "Afectación Severa ($outage sin luz)"
                rotation > 0 -> "Rotación Activa"
                energized == mCircuits.size -> "Servicio Normal"
                outage == 0 -> "Sin afectación reportada ($unknown sin datos)"
                else -> "Afectación Parcial ($energized con luz, $outage sin luz, $unknown sin datos)"
            }

            MunicipalityStatus(
                name = muniName,
                totalCircuits = mCircuits.size,
                energizedCount = energized,
                outageCount = outage,
                rotationCount = rotation,
                statusSummary = statusText
            )
        }.sortedByDescending { it.totalCircuits }
    }

    fun toggleFavorite(circuitId: String) {
        _favorites.update { current ->
            if (current.contains(circuitId)) current - circuitId else current + circuitId
        }
        _circuits.update { current ->
            current.map { if (it.id == circuitId) it.copy(isFavorite = !it.isFavorite) else it }
        }
    }

    suspend fun scrapeUrl(url: String): Result<ElectricalReport> {
        _isLoading.value = true
        _lastScrapedUrl.value = url
        try {
            // Antes de leer nada, los datos viejos dejan de presentarse como vigentes
            _circuits.update { GridUpdater.expireStale(it, System.currentTimeMillis()) }

            val reports = parserEngine.fetchChannelReports(url).getOrElse { return Result.failure(it) }

            // Solo se procesan los mensajes que no se habían visto (reports va de más antiguo a más nuevo)
            val fresh = reports.filter { it.postId == null || processedPostIds.add(it.postId) }
            fresh.forEach { applyReportToGrid(it) }
            if (fresh.isNotEmpty()) _reports.update { fresh.asReversed() + it }

            return Result.success(reports.last())
        } finally {
            _isLoading.value = false
        }
    }

    fun parseRawText(text: String, sourceName: String = "Texto Ingresado"): ElectricalReport {
        val newReport = parserEngine.parseReportText(text, sourceUrl = sourceName)
        _reports.update { listOf(newReport) + it }
        applyReportToGrid(newReport)
        return newReport
    }

    fun addCommunityReport(userAlias: String, municipality: String, circuitName: String, status: CircuitStatus, comment: String) {
        val newRep = CommunityReport(
            id = UUID.randomUUID().toString(),
            userAlias = userAlias.ifBlank { "Ciudadano" },
            municipality = municipality,
            circuitName = circuitName,
            status = status,
            timestamp = "Justo ahora",
            comment = comment
        )
        _communityReports.update { listOf(newRep) + it }
    }

    private fun applyReportToGrid(report: ElectricalReport) {
        _circuits.update { GridUpdater.applyReport(it, report, System.currentTimeMillis()) }
    }
}

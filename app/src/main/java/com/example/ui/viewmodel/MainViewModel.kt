package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CircuitInfo
import com.example.data.model.CircuitStatus
import com.example.data.model.CommunityReport
import com.example.data.model.ElectricalReport
import com.example.data.model.MunicipalityStatus
import com.example.data.repository.ElectricalGridRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScraperUiState {
    data object Idle : ScraperUiState
    data object Loading : ScraperUiState
    data class Success(val report: ElectricalReport) : ScraperUiState
    data class Error(val message: String) : ScraperUiState
}

class MainViewModel @JvmOverloads constructor(
    private val repository: ElectricalGridRepository = ElectricalGridRepository()
) : ViewModel() {

    val circuits: StateFlow<List<CircuitInfo>> = repository.circuits
    val reports: StateFlow<List<ElectricalReport>> = repository.reports
    val communityReports: StateFlow<List<CommunityReport>> = repository.communityReports
    val favorites: StateFlow<Set<String>> = repository.favorites
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val lastScrapedUrl: StateFlow<String> = repository.lastScrapedUrl

    private val _scraperUiState = MutableStateFlow<ScraperUiState>(ScraperUiState.Idle)
    val scraperUiState: StateFlow<ScraperUiState> = _scraperUiState.asStateFlow()

    private val _autoRefreshMinutes = MutableStateFlow(30) // Default 30 min
    val autoRefreshMinutes: StateFlow<Int> = _autoRefreshMinutes.asStateFlow()

    private val _lastScrapedTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastScrapedTimestamp: StateFlow<Long> = _lastScrapedTimestamp.asStateFlow()

    private val _countdownText = MutableStateFlow("Próximo en 30:00 min")
    val countdownText: StateFlow<String> = _countdownText.asStateFlow()

    private val _lastScrapedTimeFormatted = MutableStateFlow("Reciente")
    val lastScrapedTimeFormatted: StateFlow<String> = _lastScrapedTimeFormatted.asStateFlow()

    init {
        startAutoRefreshScheduler()
        scrapeChannel(lastScrapedUrl.value)
    }

    private fun startAutoRefreshScheduler() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                val intervalMin = _autoRefreshMinutes.value
                val now = System.currentTimeMillis()
                val last = _lastScrapedTimestamp.value

                if (intervalMin <= 0) {
                    _countdownText.value = "Solo Manual (A demanda)"
                } else {
                    val intervalMs = intervalMin * 60 * 1000L
                    val elapsed = now - last
                    val remainingMs = intervalMs - elapsed

                    if (remainingMs <= 0) {
                        _countdownText.value = "Sincronizando..."
                        scrapeChannel(lastScrapedUrl.value)
                    } else {
                        val minutesLeft = remainingMs / (60 * 1000)
                        val secondsLeft = (remainingMs % (60 * 1000)) / 1000
                        _countdownText.value = String.format(java.util.Locale.getDefault(), "Próximo en %02d:%02d min", minutesLeft, secondsLeft)
                    }
                }

                // Format elapsed time string
                val minutesSince = (now - last) / (60 * 1000)
                _lastScrapedTimeFormatted.value = when {
                    minutesSince <= 0 -> "Hace un momento"
                    minutesSince == 1L -> "Hace 1 minuto"
                    minutesSince < 60 -> "Hace $minutesSince minutos"
                    else -> "Hace ${minutesSince / 60} horas"
                }
            }
        }
    }

    fun setAutoRefreshInterval(minutes: Int) {
        _autoRefreshMinutes.value = minutes
        if (minutes > 0) {
            _lastScrapedTimestamp.value = System.currentTimeMillis()
        }
    }

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMunicipality = MutableStateFlow<String?>(null)
    val selectedMunicipality: StateFlow<String?> = _selectedMunicipality.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<CircuitStatus?>(null)
    val selectedStatusFilter: StateFlow<CircuitStatus?> = _selectedStatusFilter.asStateFlow()

    private val _selectedCircuitForDetail = MutableStateFlow<CircuitInfo?>(null)
    val selectedCircuitForDetail: StateFlow<CircuitInfo?> = _selectedCircuitForDetail.asStateFlow()

    val filteredCircuits: StateFlow<List<CircuitInfo>> = combine(
        circuits,
        searchQuery,
        selectedMunicipality,
        selectedStatusFilter
    ) { circuitList, query, muni, status ->
        circuitList.filter { c ->
            val matchesQuery = query.isBlank() || c.name.contains(query, ignoreCase = true) || c.municipality.contains(query, ignoreCase = true) || c.substation.contains(query, ignoreCase = true)
            val matchesMuni = muni == null || c.municipality.equals(muni, ignoreCase = true)
            val matchesStatus = status == null || c.status == status
            matchesQuery && matchesMuni && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedMunicipality(muni: String?) {
        _selectedMunicipality.value = muni
    }

    fun setSelectedStatusFilter(status: CircuitStatus?) {
        _selectedStatusFilter.value = status
    }

    fun setSelectedCircuitForDetail(circuit: CircuitInfo?) {
        _selectedCircuitForDetail.value = circuit
    }

    fun toggleFavorite(circuitId: String) {
        repository.toggleFavorite(circuitId)
    }

    fun getMunicipalityStatuses(): List<MunicipalityStatus> {
        return repository.getMunicipalityStatuses()
    }

    private var scrapeJob: Job? = null

    fun scrapeChannel(url: String) {
        // Evita lanzar descargas en paralelo (el temporizador revisa cada segundo mientras una descarga sigue en curso)
        if (scrapeJob?.isActive == true) return
        scrapeJob = viewModelScope.launch {
            _scraperUiState.value = ScraperUiState.Loading
            try {
                val result = repository.scrapeUrl(url)
                result.onSuccess { report ->
                    _scraperUiState.value = ScraperUiState.Success(report)
                }.onFailure { error ->
                    _scraperUiState.value = ScraperUiState.Error(error.localizedMessage ?: "Error al conectar con la fuente")
                }
            } catch (e: Exception) {
                _scraperUiState.value = ScraperUiState.Error(e.localizedMessage ?: "Error al conectar con la fuente")
            } finally {
                _lastScrapedTimestamp.value = System.currentTimeMillis()
            }
        }
    }

    fun parseCustomText(text: String) {
        viewModelScope.launch {
            _scraperUiState.value = ScraperUiState.Loading
            try {
                val report = repository.parseRawText(text, "Comunicado Ingresado Manualmente")
                _scraperUiState.value = ScraperUiState.Success(report)
            } catch (e: Exception) {
                _scraperUiState.value = ScraperUiState.Error("Error al procesar texto: ${e.localizedMessage}")
            }
        }
    }

    fun addCommunityReport(userAlias: String, municipality: String, circuitName: String, status: CircuitStatus, comment: String) {
        repository.addCommunityReport(userAlias, municipality, circuitName, status, comment)
    }
}

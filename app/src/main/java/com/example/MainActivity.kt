package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Announcement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CyanElectric
import com.example.ui.components.CircuitDetailModal
import com.example.ui.screens.CircuitsScreen
import com.example.ui.screens.CommunityReportsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsHistoryScreen
import com.example.ui.screens.ScraperScreen
import com.example.ui.theme.GranmaElectricaTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GranmaElectricaTheme {
                GranmaElectricaApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GranmaElectricaApp(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val circuits by viewModel.filteredCircuits.collectAsStateWithLifecycle()
    val rawCircuits by viewModel.circuits.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val communityReports by viewModel.communityReports.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedMunicipality by viewModel.selectedMunicipality.collectAsStateWithLifecycle()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val selectedCircuitDetail by viewModel.selectedCircuitForDetail.collectAsStateWithLifecycle()
    val scraperUiState by viewModel.scraperUiState.collectAsStateWithLifecycle()
    val lastScrapedUrl by viewModel.lastScrapedUrl.collectAsStateWithLifecycle()
    val autoRefreshMinutes by viewModel.autoRefreshMinutes.collectAsStateWithLifecycle()
    val countdownText by viewModel.countdownText.collectAsStateWithLifecycle()
    val lastScrapedFormatted by viewModel.lastScrapedTimeFormatted.collectAsStateWithLifecycle()

    val municipalities = viewModel.getMunicipalityStatuses()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanElectric.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = CyanElectric,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Granma Eléctrica",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sistema Eléctrico Provincial",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanElectric
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.scrapeChannel(lastScrapedUrl) },
                        modifier = Modifier.testTag("topbar_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar ahora",
                            tint = CyanElectric
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = CyanElectric,
                    indicatorColor = CyanElectric,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    colors = itemColors,
                    modifier = Modifier.testTag("tab_dashboard")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = { Icon(Icons.Default.ElectricBolt, contentDescription = "Circuitos") },
                    label = { Text("Circuitos") },
                    colors = itemColors,
                    modifier = Modifier.testTag("tab_circuits")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = "Actualizar") },
                    label = { Text("Actualizar") },
                    colors = itemColors,
                    modifier = Modifier.testTag("tab_actualizar")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = { Icon(Icons.Outlined.Announcement, contentDescription = "Partes") },
                    label = { Text("Partes") },
                    colors = itemColors,
                    modifier = Modifier.testTag("tab_reports")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setSelectedTab(4) },
                    icon = { Icon(Icons.Default.People, contentDescription = "Comunidad") },
                    label = { Text("Comunidad") },
                    colors = itemColors,
                    modifier = Modifier.testTag("tab_community")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    circuits = rawCircuits,
                    reports = reports,
                    municipalities = municipalities,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onNavigateToCircuits = { muni ->
                        viewModel.setSelectedMunicipality(muni)
                        viewModel.setSelectedTab(1)
                    },
                    onNavigateToScraper = { viewModel.setSelectedTab(2) },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onCircuitClick = { viewModel.setSelectedCircuitForDetail(it) }
                )

                1 -> CircuitsScreen(
                    circuits = circuits,
                    searchQuery = searchQuery,
                    selectedMunicipality = selectedMunicipality,
                    selectedStatusFilter = selectedStatusFilter,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onMunicipalitySelect = { viewModel.setSelectedMunicipality(it) },
                    onStatusFilterSelect = { viewModel.setSelectedStatusFilter(it) },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onCircuitClick = { viewModel.setSelectedCircuitForDetail(it) }
                )

                2 -> ScraperScreen(
                    currentUrl = lastScrapedUrl,
                    scraperState = scraperUiState,
                    autoRefreshMinutes = autoRefreshMinutes,
                    countdownText = countdownText,
                    lastScrapedFormatted = lastScrapedFormatted,
                    onScrapeUrl = { viewModel.scrapeChannel(it) },
                    onParseText = { viewModel.parseCustomText(it) },
                    onUpdateAutoRefreshInterval = { viewModel.setAutoRefreshInterval(it) }
                )

                3 -> ReportsHistoryScreen(reports = reports)

                4 -> CommunityReportsScreen(
                    reports = communityReports,
                    onSubmitReport = { alias, muni, circ, status, comment ->
                        viewModel.addCommunityReport(alias, muni, circ, status, comment)
                    }
                )
            }
        }
    }

    if (selectedCircuitDetail != null) {
        CircuitDetailModal(
            circuit = selectedCircuitDetail!!,
            onFavoriteToggle = { viewModel.toggleFavorite(selectedCircuitDetail!!.id) },
            onDismiss = { viewModel.setSelectedCircuitForDetail(null) }
        )
    }
}

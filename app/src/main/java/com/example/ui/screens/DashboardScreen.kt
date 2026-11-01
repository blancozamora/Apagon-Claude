package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CircuitInfo
import com.example.data.model.CircuitStatus
import com.example.data.model.ElectricalReport
import com.example.data.model.MunicipalityStatus
import com.example.ui.components.CircuitCard
import com.example.ui.components.MetricCard
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.StatusEnergized
import com.example.ui.theme.StatusOutage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    circuits: List<CircuitInfo>,
    reports: List<ElectricalReport>,
    municipalities: List<MunicipalityStatus>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToCircuits: (String?) -> Unit,
    onNavigateToScraper: () -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onCircuitClick: (CircuitInfo) -> Unit
) {
    val totalCircuits = circuits.size
    val energizedCount = circuits.count { it.status == CircuitStatus.ENERGIZED }
    val outageCount = circuits.count { it.status == CircuitStatus.OUTAGE }
    val knownCount = energizedCount + outageCount
    val energizedPercentage = if (knownCount > 0) (energizedCount * 100) / knownCount else 0

    val latestReport = reports.firstOrNull()
    val deficitDisplay = latestReport?.generationDeficitMW?.let { "$it MW" } ?: "Sin datos"
    val deficitSubtitle = if (latestReport != null) "Según comunicado oficial" else "Pendiente de sincronizar"
    val favoritesList = circuits.filter { it.isFavorite }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Banner: without fixed height restriction
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.hero_grid_banner_1793554005246),
                        contentDescription = "Monitoreo Granma Eléctrica",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (latestReport != null) "PARTE EN VIVO DISPONIBLE" else "ESPERANDO ACTUALIZACIÓN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Granma Eléctrica",
                            fontSize = 22.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (latestReport != null) "Datos actualizados: ${latestReport.date}" else "Sincroniza con el canal oficial para ver el estado real",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Quick Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_search_input"),
                placeholder = { Text("Buscar circuito (ej. Circuito 1, Bayamo)...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", modifier = Modifier.size(20.dp)) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }

        // Provincial Energy Overview Gauge
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cobertura de Servicio Granma",
                                fontSize = 15.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (knownCount > 0) "$energizedCount de $knownCount circuitos con servicio" else "Sin partes procesados aún",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (knownCount > 0) "$energizedPercentage%" else "--",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (knownCount > 0 && energizedPercentage >= 50) StatusEnergized else if (knownCount > 0) StatusOutage else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { if (knownCount > 0) energizedPercentage / 100f else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (energizedPercentage >= 50) StatusEnergized else StatusOutage,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(StatusEnergized)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$energizedCount Energizados",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(StatusOutage)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$outageCount Sin Servicio",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Metrics Grid (Adaptive: stacks vertically if width < 360dp, row if wider)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isNarrow = maxWidth < 360.dp

                if (isNarrow) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            title = "Déficit de Generación",
                            value = deficitDisplay,
                            subtitle = deficitSubtitle,
                            icon = Icons.Default.Speed,
                            iconTint = MaterialTheme.colorScheme.secondary
                        )

                        MetricCard(
                            title = "Circuitos en Apagón",
                            value = if (knownCount > 0) "$outageCount" else "--",
                            subtitle = if (knownCount > 0) "Afectación confirmada" else "Pendiente de parte",
                            icon = Icons.Default.FlashOff,
                            iconTint = StatusOutage
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Déficit de Generación",
                            value = deficitDisplay,
                            subtitle = deficitSubtitle,
                            icon = Icons.Default.Speed,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )

                        MetricCard(
                            title = "Circuitos en Apagón",
                            value = if (knownCount > 0) "$outageCount" else "--",
                            subtitle = if (knownCount > 0) "Afectación confirmada" else "Pendiente de parte",
                            icon = Icons.Default.FlashOff,
                            iconTint = StatusOutage,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Latest Official Announcement Banner
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToScraper() }
                    .testTag("latest_report_banner")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Actualizar Estado (Telegram / Twitter)",
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = latestReport?.aiSummary ?: "No hay comunicados registrados aún. Toca aquí para sincronizar con Telegram o pegar el parte oficial de hoy.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Granma Municipalities Status Overview Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estado por Municipio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ver todos",
                    fontSize = 13.sp,
                    color = CyanElectric,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToCircuits(null) }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .testTag("see_all_circuits")
                )
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(municipalities) { muni ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .width(190.dp)
                            .heightIn(min = 120.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            .clickable { onNavigateToCircuits(muni.name) }
                            .testTag("muni_card_${muni.name}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = muni.name,
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = muni.statusSummary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = if (muni.outageCount > 0) StatusOutage else StatusEnergized,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Column {
                                LinearProgressIndicator(
                                    progress = { muni.percentageEnergized / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = StatusEnergized,
                                    trackColor = StatusOutage.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${muni.energizedCount}/${muni.totalCircuits} energizados",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Favorites Section if any
        if (favoritesList.isNotEmpty()) {
            item {
                Text(
                    text = "Mis Circuitos Favoritos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(favoritesList, key = { "fav_${it.id}" }) { circuit ->
                CircuitCard(
                    circuit = circuit,
                    onFavoriteToggle = { onFavoriteToggle(circuit.id) },
                    onClick = { onCircuitClick(circuit) }
                )
            }
        }

        // Bottom extra spacing so last element is never covered
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

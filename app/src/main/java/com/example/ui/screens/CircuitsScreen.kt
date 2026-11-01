package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CircuitInfo
import com.example.data.model.CircuitStatus
import com.example.ui.components.CircuitCard

@Composable
fun CircuitsScreen(
    circuits: List<CircuitInfo>,
    searchQuery: String,
    selectedMunicipality: String?,
    selectedStatusFilter: CircuitStatus?,
    onSearchQueryChange: (String) -> Unit,
    onMunicipalitySelect: (String?) -> Unit,
    onStatusFilterSelect: (CircuitStatus?) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onCircuitClick: (CircuitInfo) -> Unit
) {
    val municipalities = listOf(
        "Bayamo", "Manzanillo", "Jiguaní", "Niquero", "Campechuela",
        "Guisa", "Media Luna", "Pilón", "Bartolomé Masó", "Yara",
        "Río Cauto", "Buey Arriba", "Cauto Cristo"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("circuits_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Circuitos de Granma",
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${circuits.size} circuitos filtrados",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Search Input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("circuits_search_input"),
                placeholder = { Text("Buscar por circuito, municipio o subestación...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", modifier = Modifier.size(20.dp)) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }

        // Municipality Filter Chips (Scrollable LazyRow to prevent any clipping)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Filtrar por Municipio:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedMunicipality == null,
                            onClick = { onMunicipalitySelect(null) },
                            label = { Text("Todos los Municipios", fontSize = 12.sp) },
                            modifier = Modifier.testTag("muni_filter_all")
                        )
                    }
                    items(municipalities) { muni ->
                        FilterChip(
                            selected = selectedMunicipality == muni,
                            onClick = { onMunicipalitySelect(if (selectedMunicipality == muni) null else muni) },
                            label = { Text(muni, fontSize = 12.sp) },
                            modifier = Modifier.testTag("muni_filter_$muni")
                        )
                    }
                }
            }
        }

        // Status Filter Chips (Scrollable LazyRow so all 4 chips fit comfortably on any screen)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Filtrar por Estado Eléctrico:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == null,
                            onClick = { onStatusFilterSelect(null) },
                            label = { Text("Todos", fontSize = 12.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == CircuitStatus.ENERGIZED,
                            onClick = { onStatusFilterSelect(if (selectedStatusFilter == CircuitStatus.ENERGIZED) null else CircuitStatus.ENERGIZED) },
                            label = { Text("Con Luz 🟢", fontSize = 12.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == CircuitStatus.OUTAGE,
                            onClick = { onStatusFilterSelect(if (selectedStatusFilter == CircuitStatus.OUTAGE) null else CircuitStatus.OUTAGE) },
                            label = { Text("Apagón 🔴", fontSize = 12.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == CircuitStatus.UNKNOWN,
                            onClick = { onStatusFilterSelect(if (selectedStatusFilter == CircuitStatus.UNKNOWN) null else CircuitStatus.UNKNOWN) },
                            label = { Text("En Espera 🔵", fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        // Circuit Cards List
        if (circuits.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron circuitos con los filtros seleccionados.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(circuits, key = { it.id }) { circuit ->
                CircuitCard(
                    circuit = circuit,
                    onFavoriteToggle = { onFavoriteToggle(circuit.id) },
                    onClick = { onCircuitClick(circuit) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

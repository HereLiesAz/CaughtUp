package com.hereliesaz.cleanunderwear.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.cleanunderwear.data.Target
import com.hereliesaz.cleanunderwear.data.TargetStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetListScreen(
    viewModel: MainViewModel,
    onTargetClick: (Int) -> Unit
) {
    val targets by viewModel.targets.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showIgnored by viewModel.showIgnored.collectAsState()
    var showSortMenu by rememberSaveable { mutableStateOf(false) }
    var selectedTargetIdForActions by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedTargetForActions = targets.find { it.id == selectedTargetIdForActions }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The Registry") },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        MainViewModel.SortOrder.values().forEach { order ->
                            DropdownMenuItem(
                                text = { Text("Sort by ${order.name.lowercase().replaceFirstChar { it.uppercase() }}") },
                                onClick = { 
                                    viewModel.setSortOrder(order)
                                    showSortMenu = false
                                }
                            )
                        }
                        Divider()
                        DropdownMenuItem(
                            text = { Text(if (showIgnored) "Hide Ignored" else "Show Ignored") },
                            onClick = { 
                                viewModel.toggleShowIgnored()
                                showSortMenu = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.triggerManualInterrogation() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Force Interrogation")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Find someone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium
            )

            val selectedSources by viewModel.selectedSources.collectAsState()
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Google", "Meta", "Apple", "Local").forEach { source ->
                    FilterChip(
                        selected = selectedSources.contains(source == "Local" ? "Device" : source),
                        onClick = { viewModel.toggleSource(if (source == "Local") "Device" else source) },
                        label = { Text(source) }
                    )
                }
            }

            if (targets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("The registry is currently empty.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(targets, key = { it.id }) { target ->
                        TargetItem(
                            target = target, 
                            onClick = { onTargetClick(target.id) },
                            onLongClick = { selectedTargetIdForActions = target.id }
                        )
                    }
                }
            }
        }
    }

    if (selectedTargetForActions != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedTargetIdForActions = null },
            sheetState = sheetState
        ) {
            TargetActionMenu(
                target = selectedTargetForActions,
                onAction = { action ->
                    selectedTargetIdForActions = null
                },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun TargetActionMenu(
    target: Target,
    onAction: (String) -> Unit,
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        if (target.status == TargetStatus.IGNORED) {
            ListItem(
                headlineContent = { Text("Resume Monitoring") },
                modifier = Modifier.clickable { 
                    viewModel.restoreTarget(target)
                    onAction("restore")
                }
            )
        } else {
            ListItem(
                headlineContent = { Text("Archive & Stop Monitoring") },
                modifier = Modifier.clickable { 
                    viewModel.ignoreTarget(target)
                    onAction("ignore")
                }
            )
        }
        ListItem(
            headlineContent = { Text("Update Information") },
            modifier = Modifier.clickable { onAction("update") }
        )
        ListItem(
            headlineContent = { Text("View Source Accounts") },
            supportingContent = { Text(target.sourceAccount ?: "Unknown") },
            modifier = Modifier.clickable { onAction("sources") }
        )
        ListItem(
            headlineContent = { Text("View Verification Sites") },
            modifier = Modifier.clickable { onAction("sites") }
        )
        ListItem(
            headlineContent = { Text("Change Frequency (${target.checkFrequencyHours}h)") },
            modifier = Modifier.clickable { onAction("frequency") }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TargetItem(target: Target, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = target.displayName, 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = target.phoneNumber, 
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Area Code: ${target.areaCode}", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            StatusBadge(status = target.status)
        }
    }
}

@Composable
fun StatusBadge(status: TargetStatus) {
    val (color, text) = when (status) {
        TargetStatus.AT_LARGE -> MaterialTheme.colorScheme.primary to "Monitoring"
        TargetStatus.INCARCERATED -> MaterialTheme.colorScheme.error to "Incarcerated"
        TargetStatus.DECEASED -> MaterialTheme.colorScheme.outline to "Deceased"
        TargetStatus.IGNORED -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) to "Archived"
        TargetStatus.UNKNOWN -> MaterialTheme.colorScheme.secondary to "Checking..."
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

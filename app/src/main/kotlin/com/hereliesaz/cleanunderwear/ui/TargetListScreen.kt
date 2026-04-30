package com.hereliesaz.cleanunderwear.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
    val diagnosticLogs by viewModel.diagnosticLogs.collectAsState()
    val showDiagnosticLog by viewModel.showDiagnosticLog.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showIgnored by viewModel.showIgnored.collectAsState()
    val namelessFilter by viewModel.showNamelessFilter.collectAsState()
    val emailOnlyFilter by viewModel.showEmailOnlyFilter.collectAsState()
    val hasEmailFilter by viewModel.hasEmailFilter.collectAsState()
    val hasAddressFilter by viewModel.hasAddressFilter.collectAsState()
    val googleFilter by viewModel.googleFilter.collectAsState()
    val metaFilter by viewModel.metaFilter.collectAsState()
    val appleFilter by viewModel.appleFilter.collectAsState()
    val deviceFilter by viewModel.deviceFilter.collectAsState()
    val showManualEntryDialog by viewModel.showManualEntryDialog.collectAsState()

    var showSortMenu by rememberSaveable { mutableStateOf(false) }
    var selectedTargetIdForActions by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedTargetForActions = targets.find { it.id == selectedTargetIdForActions }
    val sheetState = rememberModalBottomSheetState()
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Was their underwear clean?") },
                    actions = {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            MainViewModel.SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text("Sort by ${order.name.lowercase().replaceFirstChar { it.uppercase() }}") },
                                    onClick = { 
                                        viewModel.setSortOrder(order)
                                        showSortMenu = false
                                    }
                                )
                            }
                            HorizontalDivider()
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

                if (operationState.isRunning) {
                    LinearProgressIndicator(
                        progress = { if (operationState.progress >= 0f) operationState.progress else 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.secondaryContainer,
                    )
                    
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = operationState.description,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Account Source Filters
                TriStateFilterChip(
                    state = googleFilter,
                    onToggle = { viewModel.setGoogleFilter(it) },
                    label = "Google"
                )
                TriStateFilterChip(
                    state = metaFilter,
                    onToggle = { viewModel.setMetaFilter(it) },
                    label = "Meta"
                )
                TriStateFilterChip(
                    state = appleFilter,
                    onToggle = { viewModel.setAppleFilter(it) },
                    label = "Apple"
                )
                TriStateFilterChip(
                    state = deviceFilter,
                    onToggle = { viewModel.setDeviceFilter(it) },
                    label = "Local"
                )

                VerticalDivider(modifier = Modifier.height(32.dp).align(Alignment.CenterVertically))

                // Nameless Filter
                TriStateFilterChip(
                    state = namelessFilter,
                    onToggle = { viewModel.setNamelessFilter(it) },
                    label = "Nameless"
                )
                
                // Email Only Filter
                TriStateFilterChip(
                    state = emailOnlyFilter,
                    onToggle = { viewModel.setEmailOnlyFilter(it) },
                    label = "Email Only"
                )

                // Has Email Filter
                TriStateFilterChip(
                    state = hasEmailFilter,
                    onToggle = { viewModel.setEmailFilter(it) },
                    label = "Email"
                )
                
                // Has Address Filter
                TriStateFilterChip(
                    state = hasAddressFilter,
                    onToggle = { viewModel.setAddressFilter(it) },
                    label = "Address"
                )
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
                    modifier = Modifier.weight(1f),
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

            if (showDiagnosticLog) {
                DiagnosticLogView(logs = diagnosticLogs)
            }
        }
    }

    if (showManualEntryDialog) {
        ManualEntryDialog(
            onDismiss = { viewModel.setShowManualEntryDialog(false) },
            onConfirm = { name, phone, email ->
                viewModel.addManualTarget(name, phone, email)
                viewModel.setShowManualEntryDialog(false)
            }
        )
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
                target.phoneNumber?.let {
                    Text(
                        text = it, 
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                target.email?.let {
                    Text(
                        text = it, 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                target.areaCode?.let {
                    if (it != "LOCAL") {
                        Text(
                            text = "Area Code: $it", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Local Number", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            
            StatusBadge(status = target.status)
        }
    }
}

@Composable
fun TriStateFilterChip(
    state: Boolean?,
    onToggle: (Boolean?) -> Unit,
    label: String
) {
    FilterChip(
        selected = state != null,
        onClick = {
            val nextState = when (state) {
                null -> true   // Only
                true -> false  // Exclude
                false -> null  // All
            }
            onToggle(nextState)
        },
        label = {
            Text(
                text = when (state) {
                    true -> "Only $label"
                    false -> "No $label"
                    null -> label
                }
            )
        },
        leadingIcon = if (state != null) {
            {
                Icon(
                    imageVector = if (state) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else null
    )
}

@Composable
fun DiagnosticLogView(logs: List<com.hereliesaz.cleanunderwear.util.DiagnosticLogger.LogEntry>) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        tonalElevation = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "UNDER-THE-HOOD ACTIVITY",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    IconButton(
                        onClick = {
                            val fullLog = logs.joinToString("\n") { "[${it.timestamp}] ${it.message}" }
                            scope.launch {
                                clipboard.setClipEntry(androidx.compose.ui.platform.ClipEntry(android.content.ClipData.newPlainText("Intelligence Log", fullLog)))
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Log", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = {
                            val fullLog = logs.joinToString("\n") { "[${it.timestamp}] ${it.message}" }
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, fullLog)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Intelligence Log"))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Log", modifier = Modifier.size(16.dp))
                    }
                }
            }
            HorizontalDivider()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(logs) { entry ->
                    val color = when (entry.level) {
                        com.hereliesaz.cleanunderwear.util.DiagnosticLogger.LogEntry.LogLevel.ERROR -> MaterialTheme.colorScheme.error
                        com.hereliesaz.cleanunderwear.util.DiagnosticLogger.LogEntry.LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = "[${entry.timestamp}] ${entry.message}",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = androidx.compose.ui.unit.TextUnit.Unspecified, // Uses default which is fine
                            color = color
                        ),
                        modifier = Modifier.padding(vertical = 1.dp),
                        fontSize = MaterialTheme.typography.labelSmall.fontSize
                    )
                }
            }
        }
    }
}

@Composable
fun ManualEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Intelligence Ingestion") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Manually add a target that Meta or Apple refuses to sync with this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Target Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Intelligence Email") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(name, phone.takeIf { it.isNotBlank() }, email.takeIf { it.isNotBlank() })
                },
                enabled = name.isNotBlank() && (phone.isNotBlank() || email.isNotBlank())
            ) {
                Text("Ingest Target")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StatusBadge(status: TargetStatus) {
    val (color, text) = when (status) {
        TargetStatus.MONITORING -> MaterialTheme.colorScheme.primary to "Monitoring"
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

package com.hereliesaz.cleanunderwear.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.*
import com.hereliesaz.aznavrail.model.AzButtonShape
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
    val operationState by viewModel.operationState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val showIgnored by viewModel.showIgnored.collectAsState()
    val namelessFilter by viewModel.showNamelessFilter.collectAsState()
    val emailOnlyFilter by viewModel.showEmailOnlyFilter.collectAsState()
    val hasEmailFilter by viewModel.hasEmailFilter.collectAsState()
    val hasAddressFilter by viewModel.hasAddressFilter.collectAsState()
    val googleFilter by viewModel.googleFilter.collectAsState()
    val metaFilter by viewModel.metaFilter.collectAsState()
    val appleFilter by viewModel.appleFilter.collectAsState()
    val deviceFilter by viewModel.deviceFilter.collectAsState()
    val pendingEnrichmentFilter by viewModel.pendingEnrichmentFilter.collectAsState()
    val showManualEntryDialog by viewModel.showManualEntryDialog.collectAsState()

    var selectedTargetIdForActions by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedTargetForActions = targets.find { it.id == selectedTargetIdForActions }
    val sheetState = rememberModalBottomSheetState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        AzTextBox(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            hint = "Interrogate Registry...",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            historyContext = "registry_search",
            onSubmit = { viewModel.setSearchQuery(it) }
        )

        if (operationState.isRunning) {
            LinearProgressIndicator(
                progress = { if (operationState.progress >= 0f) operationState.progress else 0f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                text = operationState.description,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sort:", style = MaterialTheme.typography.labelSmall)
            AzCycler(
                options = MainViewModel.SortOrder.entries.map { it.name },
                selectedOption = sortOrder.name,
                onCycle = { name -> 
                    viewModel.setSortOrder(MainViewModel.SortOrder.valueOf(name))
                }
            )
            
            AzToggle(
                isChecked = showIgnored,
                onToggle = { viewModel.toggleShowIgnored() },
                toggleOnText = "Hide Archived",
                toggleOffText = "Show Archived"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Account Source Filters
            TriStateFilterChip(state = googleFilter, onToggle = { viewModel.setGoogleFilter(it) }, label = "Google")
            TriStateFilterChip(state = metaFilter, onToggle = { viewModel.setMetaFilter(it) }, label = "Meta")
            TriStateFilterChip(state = appleFilter, onToggle = { viewModel.setAppleFilter(it) }, label = "Apple")
            TriStateFilterChip(state = deviceFilter, onToggle = { viewModel.setDeviceFilter(it) }, label = "Local")
            VerticalDivider(modifier = Modifier.height(32.dp).align(Alignment.CenterVertically))
            TriStateFilterChip(state = namelessFilter, onToggle = { viewModel.setNamelessFilter(it) }, label = "Nameless")
            TriStateFilterChip(state = emailOnlyFilter, onToggle = { viewModel.setEmailOnlyFilter(it) }, label = "Email Only")
            TriStateFilterChip(state = hasEmailFilter, onToggle = { viewModel.setEmailFilter(it) }, label = "Email")
            TriStateFilterChip(state = hasAddressFilter, onToggle = { viewModel.setAddressFilter(it) }, label = "Address")
            VerticalDivider(modifier = Modifier.height(32.dp).align(Alignment.CenterVertically))
            TriStateFilterChip(
                state = pendingEnrichmentFilter,
                onToggle = { viewModel.setPendingEnrichmentFilter(it) },
                label = "Pending Enrichment"
            )
        }

        if (targets.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("The surveillance ledger is currently blank.")
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
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
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
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = MaterialTheme.colorScheme.primary,
        textContentColor = MaterialTheme.colorScheme.onBackground,
        title = { Text("Manual Intelligence Ingestion") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Manually add a target that Meta or Apple refuses to sync with this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                AzTextBox(
                    value = name,
                    onValueChange = { name = it },
                    hint = "Target Name",
                    modifier = Modifier.fillMaxWidth(),
                    onSubmit = {}
                )
                AzTextBox(
                    value = phone,
                    onValueChange = { phone = it },
                    hint = "Phone Number",
                    modifier = Modifier.fillMaxWidth(),
                    onSubmit = {}
                )
                AzTextBox(
                    value = email,
                    onValueChange = { email = it },
                    hint = "Intelligence Email",
                    modifier = Modifier.fillMaxWidth(),
                    onSubmit = {}
                )
                
                AzButton(
                    text = "Ingest Target",
                    onClick = { onConfirm(name, phone.takeIf { it.isNotBlank() }, email.takeIf { it.isNotBlank() }) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    enabled = name.isNotBlank() && (phone.isNotBlank() || email.isNotBlank()),
                    shape = AzButtonShape.RECTANGLE
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StatusBadge(status: TargetStatus) {
    val (color: Color, text: String) = when (status) {
        TargetStatus.MONITORING -> VerifiedGreen to "Monitoring"
        TargetStatus.INCARCERATED -> WarningRed to "Incarcerated"
        TargetStatus.DECEASED -> Color.Gray to "Deceased"
        TargetStatus.IGNORED -> Color.LightGray to "Archived"
        TargetStatus.UNKNOWN -> IntelligenceGold to "Checking..."
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

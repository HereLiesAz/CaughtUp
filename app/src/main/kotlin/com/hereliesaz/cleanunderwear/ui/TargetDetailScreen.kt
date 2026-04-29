package com.hereliesaz.cleanunderwear.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.cleanunderwear.data.Target
import com.hereliesaz.cleanunderwear.data.TargetStatus
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetDetailScreen(
    target: Target,
    onUpdateTarget: (Target) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details: ${target.displayName}") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = target.displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider()

            DetailRow(label = "Contact Number", value = target.phoneNumber)
            DetailRow(label = "Location Reference", value = target.areaCode)
            if (!target.residenceInfo.isNullOrBlank()) {
                DetailRow(label = "Known Home Area", value = target.residenceInfo)
            }
            DetailRow(
                label = "Latest Status", 
                value = target.status.name.lowercase().replaceFirstChar { it.uppercase() }
            )
            
            val dateString = if (target.lastScrapedTimestamp > 0) {
                SimpleDateFormat("MM/dd/yyyy hh:mm a", java.util.Locale.US)
                    .format(Date(target.lastScrapedTimestamp))
            } else {
                "Never"
            }
            DetailRow(label = "Last Registry Check", value = dateString)
            DetailRow(label = "Found In", value = target.sourceAccount ?: "Unknown")

            if (target.lockupUrl != null || target.obituaryUrl != null) {
                Text(
                    text = "Verification Sources",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                target.lockupUrl?.let { url ->
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }) {
                        Text("Public Records / Roster")
                    }
                }
                target.obituaryUrl?.let { url ->
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }) {
                        Text("Obituary Registry")
                    }
                }
            }

            HorizontalDivider()
            
            Text(
                text = "Check Frequency",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(6, 12, 24, 48).forEach { hours ->
                    FilterChip(
                        selected = target.checkFrequencyHours == hours,
                        onClick = { 
                            onUpdateTarget(target.copy(checkFrequencyHours = hours))
                        },
                        label = { Text("${hours}h") }
                    )
                }
            }

            if (target.lastVerificationSnippet != null) {
                HorizontalDivider()
                Text(
                    text = "Verification Evidence",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = target.lastVerificationSnippet,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { 
                                    // Rejected: Move back to monitoring
                                    onUpdateTarget(target.copy(
                                        status = TargetStatus.MONITORING,
                                        lastVerificationSnippet = null
                                    ))
                                }
                            ) {
                                Text("Incorrect Match / Only Mentioned")
                            }
                            Button(
                                onClick = { 
                                    // Confirmed: No action needed besides maybe clearing snippet or keeping it as proof
                                }
                            ) {
                                Text("Confirm Match")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    try {
                        val query = "${target.displayName} ${target.residenceInfo ?: ""}".trim()
                        val encodedQuery = URLEncoder.encode(query, "UTF-8")
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://news.google.com/search?q=$encodedQuery"))
                        context.startActivity(intent)
                    } catch (e: android.content.ActivityNotFoundException) {
                        // Ignore if no web browser is installed
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("General News Search")
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

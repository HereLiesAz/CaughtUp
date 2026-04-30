package com.hereliesaz.cleanunderwear.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.*
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.cleanunderwear.data.Target
import com.hereliesaz.cleanunderwear.data.TargetStatus
import com.hereliesaz.cleanunderwear.util.CyberBackgroundChecks
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
                title = { Text("Intelligence Profile: ${target.displayName}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = target.displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider()

            DetailRow(label = "Intelligence Name", value = target.displayName)
            
            HorizontalDivider()

            target.phoneNumber?.let { DetailRow(label = "Contact Number", value = it) }
            target.email?.let { DetailRow(label = "Intelligence Email", value = it) }
            target.areaCode?.let { DetailRow(label = "Location Reference", value = it) }
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
                    AzButton(
                        text = "Public Records / Roster",
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AzButtonShape.RECTANGLE
                    )
                }
                target.obituaryUrl?.let { url ->
                    AzButton(
                        text = "Obituary Registry",
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AzButtonShape.RECTANGLE
                    )
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
                AzRoller(
                    options = listOf(6, 12, 24, 48).map { "${it}h" },
                    selectedOption = "${target.checkFrequencyHours}h",
                    onOptionSelected = { hoursText ->
                        val hours = hoursText.removeSuffix("h").toInt()
                        onUpdateTarget(target.copy(checkFrequencyHours = hours))
                    }
                )
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
                            AzButton(
                                text = "Incorrect Match",
                                onClick = { 
                                    onUpdateTarget(target.copy(
                                        status = TargetStatus.MONITORING,
                                        lastVerificationSnippet = null
                                    ))
                                },
                                modifier = Modifier.weight(1f),
                                shape = AzButtonShape.RECTANGLE
                            )
                            AzButton(
                                text = "Confirm Match",
                                onClick = { 
                                    // Confirmed
                                },
                                modifier = Modifier.weight(1f),
                                shape = AzButtonShape.RECTANGLE
                            )
                        }
                    }
                }
            }

            HorizontalDivider()
            
            Text(
                text = "External Deep Interrogation",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CyberBackgroundChecks.getNameSearchUrl(target.displayName)))) },
                    label = { Text("Name Check") }
                )
                
                target.phoneNumber?.let { phone ->
                    AssistChip(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CyberBackgroundChecks.getPhoneSearchUrl(phone)))) },
                        label = { Text("Phone Check") }
                    )
                }

                target.email?.let { email ->
                    AssistChip(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CyberBackgroundChecks.getEmailSearchUrl(email)))) },
                        label = { Text("Email Check") }
                    )
                }

                target.residenceInfo?.let { addr ->
                    AssistChip(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CyberBackgroundChecks.getAddressSearchUrl(addr)))) },
                        label = { Text("Address Check") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AzButton(
                text = "General News Search",
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
                modifier = Modifier.fillMaxWidth(),
                shape = AzButtonShape.RECTANGLE
            )
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

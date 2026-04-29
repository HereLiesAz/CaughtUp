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
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetDetailScreen(target: Target, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dossier: ${target.displayName}") },
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

            DetailRow(label = "Primary Identifier", value = target.phoneNumber)
            DetailRow(label = "Geographic Anchor", value = target.areaCode)
            if (!target.residenceInfo.isNullOrBlank()) {
                DetailRow(label = "Known Residence", value = target.residenceInfo)
            }
            DetailRow(
                label = "Current State of Being", 
                value = target.status.name.replace("_", " ")
            )
            
            val dateString = if (target.lastScrapedTimestamp > 0) {
                SimpleDateFormat("MM/dd/yyyy hh:mm a", java.util.Locale.US)
                    .format(Date(target.lastScrapedTimestamp))
            } else {
                "Never"
            }
            DetailRow(label = "Last Interrogation of the Void", value = dateString)
            DetailRow(label = "Extraction Source", value = target.sourceAccount ?: "Unknown")

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

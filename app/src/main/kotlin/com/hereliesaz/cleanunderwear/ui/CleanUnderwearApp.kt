package com.hereliesaz.cleanunderwear.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.hereliesaz.aznavrail.*
import com.hereliesaz.aznavrail.model.*
import com.hereliesaz.cleanunderwear.data.Target

@Composable
fun CleanUnderwearApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val operationState by viewModel.operationState.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val showDiagnosticLog by viewModel.showDiagnosticLog.collectAsState()
    val diagnosticLogs by viewModel.diagnosticLogs.collectAsState()

    val activeColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background

    AzHostActivityLayout(
        navController = navController,
        modifier = Modifier.fillMaxSize(),
        currentDestination = currentDestination?.route,
        isLandscape = isLandscape,
        initiallyExpanded = false
    ) {
        azConfig(
            dockingSide = AzDockingSide.LEFT,
            packButtons = true
        )
        
        azTheme(
            activeColor = activeColor,
            translucentBackground = backgroundColor.copy(alpha = 0.90f),
            defaultShape = AzButtonShape.RECTANGLE, // Default shape for all items
        )

        azAdvanced(
            isLoading = operationState.isRunning,
            helpEnabled = true,
            helpList = mapOf(
                "registry" to "Your primary surveillance ledger.",
                "ingest" to "Manual entry for targets that evade automated harvesting.",
                "harvest" to "Deep scythe across all connected social and system databases.",
                "interrogate" to "Force a real-time check against public rosters."
            )

        )

        if (isOnboardingCompleted) {
            azRailItem(
                id = "registry",
                text = "Registry",
                route = "targetList",
                content = Icons.Default.Groups,
                info = "View the complete surveillance list."
            )

            azRailHostItem(
                id = "intelligence_ops",
                text = "Ops",
                content = Icons.Default.Analytics,
            ) {
                azRailSubItem(
                    id = "ingest",
                    text = "Ingest",
                    content = Icons.Default.PersonAdd,
                    onClick = { viewModel.setShowManualEntryDialog(true) },
                    hostId = "intelligence_ops"
                )
                azRailSubItem(
                    id = "harvest",
                    text = "Harvest",
                    content = Icons.Default.Radar,
                    onClick = { viewModel.sweepContacts() },
                    hostId = "intelligence_ops"
                )
                azRailSubItem(
                    id = "interrogate",
                    text = "Interrogate",
                    content = Icons.Default.SavedSearch,
                    onClick = { viewModel.triggerManualInterrogation() },
                    hostId = "intelligence_ops"
                )
            }

            azDivider()

            azRailItem(
                id = "settings",
                text = "Settings",
                route = "settings",
                content = Icons.Default.Settings
            )
            
            azMenuToggle(
                id = "diagnostic_log",
                isChecked = showDiagnosticLog,
                toggleOnText = "Hide Activity Log",
                toggleOffText = "Show Activity Log",
                onClick = { viewModel.setShowDiagnosticLog(!showDiagnosticLog) }
            )
        }

        background(weight = 0) {
            // Diagnostic log as a background layer that peeks through
            if (showDiagnosticLog) {
                Box(Modifier.fillMaxSize()) {
                    DiagnosticLogView(logs = diagnosticLogs)
                }
            }
        }

        onscreen {
            val startDestination = if (isOnboardingCompleted) "targetList" else "onboarding"
            
            NavHost(navController = navController, startDestination = startDestination) {
                composable("onboarding") {
                    OnboardingScreen(onComplete = {
                        viewModel.completeOnboarding()
                        navController.navigate("targetList") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    })
                }
                composable("targetList") {
                    TargetListScreen(
                        viewModel = viewModel,
                        onTargetClick = { targetId ->
                            navController.navigate("targetDetail/$targetId")
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "targetDetail/{targetId}",
                    arguments = listOf(navArgument("targetId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val targetId = backStackEntry.arguments?.getInt("targetId") ?: return@composable
                    val targets by viewModel.targets.collectAsState()
                    val target = targets.find { it.id == targetId }

                    if (target != null) {
                        TargetDetailScreen(
                            target = target,
                            onUpdateTarget = { viewModel.updateTarget(it) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

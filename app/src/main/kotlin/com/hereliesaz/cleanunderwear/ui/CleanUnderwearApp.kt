package com.hereliesaz.cleanunderwear.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.hereliesaz.aznavrail.*
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.cleanunderwear.data.Target

@Composable
fun CleanUnderwearApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activeColor = MaterialTheme.colorScheme.primary

    AzHostActivityLayout(
        navController = navController,
        modifier = Modifier.fillMaxSize(),
        currentDestination = currentDestination?.route,
        isLandscape = isLandscape,
        initiallyExpanded = false
    ) {
        if (isOnboardingCompleted) {
            azConfig(dockingSide = AzDockingSide.LEFT)
            azTheme(activeColor = activeColor)

            azRailItem(
                id = "registry",
                text = "Registry",
                route = "targetList",
                content = Icons.Default.People,
                info = "View your local surveillance registry."
            )
            
            azRailItem(
                id = "manual_entry",
                text = "Ingest",
                content = Icons.Default.Add,
                info = "Manually ingest a new target into the ledger.",
                onClick = { viewModel.setShowManualEntryDialog(true) }
            )

            azRailItem(
                id = "harvest",
                text = "Harvest",
                content = Icons.Default.PersonAdd,
                info = "Scythe through local databases for new targets.",
                onClick = { viewModel.sweepContacts() }
            )
            
            azRailItem(
                id = "interrogate",
                text = "Interrogate",
                content = Icons.Default.Refresh,
                info = "Force a real-time interrogation of municipal rosters.",
                onClick = { viewModel.triggerManualInterrogation() }
            )

            azDivider()

            azRailItem(
                id = "settings",
                text = "Settings",
                route = "settings",
                content = Icons.Default.Settings,
                info = "Configure surveillance parameters and covert mode."
            )
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

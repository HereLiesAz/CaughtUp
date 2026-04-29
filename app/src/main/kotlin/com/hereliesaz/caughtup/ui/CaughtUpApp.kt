package com.hereliesaz.caughtup.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.hereliesaz.caughtup.data.Target

@Composable
fun CaughtUpApp(viewModel: MainViewModel) {
    val navController = rememberNavController()

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "targetList") {
            composable("targetList") {
                TargetListScreen(
                    viewModel = viewModel,
                    onTargetClick = { targetId ->
                        navController.navigate("targetDetail/$targetId")
                    }
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
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

package com.hereliesaz.caughtup.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.hereliesaz.caughtup.data.Target

@Composable
fun CaughtUpApp(viewModel: MainViewModel) {
    Surface(modifier = Modifier.fillMaxSize()) {
        TargetListScreen(
            viewModel = viewModel,
            onTargetClick = { /* Handle navigation to detail screen */ }
        )
    }
}

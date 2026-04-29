package com.hereliesaz.cleanunderwear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.hereliesaz.cleanunderwear.ui.MainViewModel
import com.hereliesaz.cleanunderwear.ui.CleanUnderwearApp
import com.hereliesaz.cleanunderwear.util.GitHubCrashReporter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
        if (contactsGranted) {
            viewModel.sweepContacts()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize GitHub Crash Reporting
        setupCrashReporting()

        checkAndRequestPermissions()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LaunchedEffect(Unit) {
                        viewModel.scheduleDailyPanopticon()
                    }

                    CleanUnderwearApp(viewModel = viewModel)
                }
            }
        }
    }

    private fun setupCrashReporting() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // In production, you'd use a background service or WorkManager for this.
            // For now, we attempt a quick fire-and-forget.
            GitHubCrashReporter(this).reportCrash(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            showPermissionRationale {
                requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
            }
        }
    }

    private fun showPermissionRationale(onConfirm: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Privacy & Monitoring Access")
            .setMessage("To build your personal registry, we need access to your contacts. To alert you of status changes, we need notification access. All data remains private on your device.")
            .setPositiveButton("Grant Access") { _, _ -> onConfirm() }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}

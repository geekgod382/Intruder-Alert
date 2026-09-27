package com.example.intruder_alert

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PrankControlScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun PrankControlScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isServiceRunning by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // Launcher for Notification Permission (Required for Foreground Service on Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            tryStartPrankService(context) { isServiceRunning = true }
        } else {
            Toast.makeText(context, "Notification permission is required for the service", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher for System Overlay Permission
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        if (!hasOverlayPermission) {
            Toast.makeText(context, "Overlay permission is mandatory for background launches!", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Intruder Alert",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // 1. Overlay Permission Button (Show only if not granted)
        if (!hasOverlayPermission) {
            Button(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    overlayPermissionLauncher.launch(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text("Grant 'Display Over Other Apps' Permission")
            }
        }

        // 2. Service Toggle Button
        Button(
            onClick = {
                if (isServiceRunning) {
                    // Stop Service
                    val intent = Intent(context, ScreenMonitorService::class.java)
                    context.stopService(intent)
                    isServiceRunning = false
                } else {
                    // Start Service (Check permissions first)
                    if (!Settings.canDrawOverlays(context)) {
                        Toast.makeText(context, "Please grant overlay permission first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        tryStartPrankService(context) { isServiceRunning = true }
                    }
                }
            },
            enabled = hasOverlayPermission,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isServiceRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isServiceRunning) "Disable Prank Mode" else "Enable Prank Mode")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isServiceRunning) "Status: Active & Guarding" else "Status: Inactive",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
}

private fun tryStartPrankService(context: Context, onStarted: () -> Unit) {
    val intent = Intent(context, ScreenMonitorService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
    onStarted()
}
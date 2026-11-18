package com.example.time.ui.dashboard

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.time.accessibility.ReelGuardAccessibilityService
import com.example.time.data.PreferencesRepository
import com.example.time.ui.components.AppHeader
import com.example.time.ui.components.MainToggleCard
import com.example.time.ui.components.PrivacyDialog
import com.example.time.ui.components.PrivacySection
import com.example.time.ui.components.QuickActionsRow
import com.example.time.ui.components.StatisticsCard
import com.example.time.ui.components.StatusCard
import com.example.time.ui.diagnostics.DiagnosticsDialog
import com.example.time.ui.theme.CharcoalBackground
import com.example.time.util.AccessibilityStatusChecker
import com.example.time.util.InstagramPackageHelper

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { PreferencesRepository.getInstance(context) }

    var isBlockingEnabled by remember { mutableStateOf(prefs.isBlockingEnabled) }
    var blockedCountToday by remember { mutableIntStateOf(prefs.getBlockedCountToday()) }
    var serviceStatus by remember { mutableStateOf(AccessibilityStatusChecker.getStatus(context)) }
    var isInstagramInstalled by remember {
        mutableStateOf(InstagramPackageHelper.getInstagramInfo(context).isInstalled)
    }

    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Refresh status on lifecycle resume (e.g. after returning from Android Settings or Instagram)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serviceStatus = AccessibilityStatusChecker.getStatus(context)
                blockedCountToday = prefs.getBlockedCountToday()
                isInstagramInstalled = InstagramPackageHelper.getInstagramInfo(context).isInstalled
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // React immediately to service live-state changes
    DisposableEffect(Unit) {
        val listener = {
            serviceStatus = AccessibilityStatusChecker.getStatus(context)
            blockedCountToday = prefs.getBlockedCountToday()
        }
        ReelGuardAccessibilityService.addStatusListener(listener)
        onDispose {
            ReelGuardAccessibilityService.removeStatusListener(listener)
        }
    }

    Scaffold(
        containerColor = CharcoalBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            AppHeader()

            Spacer(modifier = Modifier.height(20.dp))

            StatusCard(
                status = serviceStatus,
                isInstagramInstalled = isInstagramInstalled,
                onEnableClicked = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            MainToggleCard(
                enabled = isBlockingEnabled,
                onToggle = { newValue ->
                    isBlockingEnabled = newValue
                    prefs.isBlockingEnabled = newValue
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatisticsCard(blockedCountToday = blockedCountToday)

            Spacer(modifier = Modifier.height(20.dp))

            QuickActionsRow(
                onEnableAccessibility = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
                onOpenInstagram = {
                    val opened = InstagramPackageHelper.openInstagram(context)
                    if (!opened) {
                        Toast.makeText(
                            context,
                            "Instagram application is not installed on this device.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onRunDiagnostics = {
                    showDiagnosticsDialog = true
                },
                onViewPrivacy = {
                    showPrivacyDialog = true
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            PrivacySection()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDiagnosticsDialog) {
        DiagnosticsDialog(
            onDismissRequest = { showDiagnosticsDialog = false }
        )
    }

    if (showPrivacyDialog) {
        PrivacyDialog(
            onDismissRequest = { showPrivacyDialog = false }
        )
    }
}


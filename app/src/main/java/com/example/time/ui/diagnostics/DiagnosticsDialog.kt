package com.example.time.ui.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.time.data.DiagnosticCaptureRepository
import com.example.time.data.PreferencesRepository
import com.example.time.detector.InstagramSignatures
import com.example.time.ui.theme.AccentMint
import com.example.time.ui.theme.AccentMintContainer
import com.example.time.ui.theme.CharcoalBackground
import com.example.time.ui.theme.CharcoalSurface
import com.example.time.ui.theme.CharcoalSurfaceBorder
import com.example.time.ui.theme.CharcoalSurfaceVariant
import com.example.time.ui.theme.TextMuted
import com.example.time.ui.theme.TextPrimary
import com.example.time.ui.theme.TextSecondary
import com.example.time.ui.theme.WarningAmber
import com.example.time.util.AccessibilityStatusChecker
import com.example.time.util.InstagramPackageHelper

@Composable
fun DiagnosticsDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesRepository.getInstance(context) }
    val igInfo = remember { InstagramPackageHelper.getInstagramInfo(context) }
    val serviceStatus = remember { AccessibilityStatusChecker.getStatus(context) }

    var captureState by remember { mutableStateOf(DiagnosticCaptureRepository.state) }
    var capturedJson by remember { mutableStateOf(DiagnosticCaptureRepository.capturedJson) }
    var capturedSnapshot by remember { mutableStateOf(DiagnosticCaptureRepository.capturedSnapshot) }

    DisposableEffect(Unit) {
        val listener = {
            captureState = DiagnosticCaptureRepository.state
            capturedJson = DiagnosticCaptureRepository.capturedJson
            capturedSnapshot = DiagnosticCaptureRepository.capturedSnapshot
        }
        DiagnosticCaptureRepository.addListener(listener)
        onDispose {
            DiagnosticCaptureRepository.removeListener(listener)
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = CharcoalBackground,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CharcoalSurfaceBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Diagnostics & Engine State",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Target Application Environment
                DiagnosticSectionHeader("ENVIRONMENT & PACKAGE")
                DiagnosticItem("Target Package", InstagramSignatures.INSTAGRAM_PACKAGE)
                DiagnosticItem("Instagram Installed", if (igInfo.isInstalled) "Yes" else "No")
                DiagnosticItem(
                    "Instagram Version",
                    if (igInfo.isInstalled) "${igInfo.versionName} (${igInfo.versionCode})" else "N/A"
                )
                DiagnosticItem("Service Connection", serviceStatus.name)

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Engine Statistics
                DiagnosticSectionHeader("RUNTIME STATS & LAST ACTION")
                DiagnosticItem("Total Detection Events", prefs.detectionCount.toString())
                DiagnosticItem("Successful Blocks", prefs.successCount.toString())
                DiagnosticItem("Failed / Unclickable", prefs.failureCount.toString())
                DiagnosticItem("Last Detection Type", prefs.lastDetectionType)
                DiagnosticItem("Last Blocking Action", prefs.lastBlockingAction)
                DiagnosticItem("Last Action Result", prefs.lastActionResult)

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Developer Snapshot Capture Tool
                DiagnosticSectionHeader("CAPTURE NEXT INSTAGRAM SCREEN")
                Text(
                    text = "Verify Instagram UI selectors on your device without transmitting data. " +
                            "Arm capture, open Instagram to the target screen, and return here to inspect.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                when (captureState) {
                    DiagnosticCaptureRepository.CaptureState.IDLE -> {
                        Button(
                            onClick = { DiagnosticCaptureRepository.armCapture() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentMint,
                                contentColor = Color(0xFF003824)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Arm Next Capture", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    DiagnosticCaptureRepository.CaptureState.AWAITING_CAPTURE -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CharcoalSurfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Awaiting Instagram Screen...",
                                    color = WarningAmber,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Switch to Instagram now and navigate to the screen you wish to inspect.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { DiagnosticCaptureRepository.disarmCapture() },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    DiagnosticCaptureRepository.CaptureState.CAPTURED -> {
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Snapshot Captured (${capturedSnapshot?.nodeCount ?: 0} nodes, max depth ${capturedSnapshot?.maxDepth ?: 0})",
                                    color = AccentMint,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("ReelGuard Sanitized UI", capturedJson ?: "")
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Sanitized JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Copy", fontSize = 11.sp, color = TextPrimary)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedButton(
                                        onClick = { DiagnosticCaptureRepository.clear() },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Clear", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CharcoalSurface)
                                    .border(1.dp, CharcoalSurfaceBorder, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = capturedJson ?: "No JSON",
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticSectionHeader(title: String) {
    Text(
        text = title,
        color = TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun DiagnosticItem(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}


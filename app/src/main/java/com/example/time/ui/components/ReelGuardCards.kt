package com.example.time.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.time.ui.theme.AccentMint
import com.example.time.ui.theme.AccentMintContainer
import com.example.time.ui.theme.CharcoalSurface
import com.example.time.ui.theme.CharcoalSurfaceBorder
import com.example.time.ui.theme.CharcoalSurfaceVariant
import com.example.time.ui.theme.DangerRed
import com.example.time.ui.theme.DangerRedContainer
import com.example.time.ui.theme.TextMuted
import com.example.time.ui.theme.TextPrimary
import com.example.time.ui.theme.TextSecondary
import com.example.time.ui.theme.WarningAmber
import com.example.time.ui.theme.WarningAmberContainer
import com.example.time.util.AccessibilityStatusChecker.ServiceStatus

@Composable
fun AppHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "ReelGuard",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Instagram without Reels.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CharcoalSurfaceVariant)
                    .border(1.dp, CharcoalSurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "LOCAL ONLY",
                    color = AccentMint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun StatusCard(
    status: ServiceStatus,
    isInstagramInstalled: Boolean,
    onEnableClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusTitle, statusDesc, dotColor, containerColor) = when {
        !isInstagramInstalled -> Quadruple(
            "Instagram Not Installed",
            "Official Instagram app was not detected on this device.",
            WarningAmber,
            WarningAmberContainer
        )
        status == ServiceStatus.ACTIVE -> Quadruple(
            "Accessibility Service Active",
            "ReelGuard is actively monitoring and protecting against Reels.",
            AccentMint,
            AccentMintContainer
        )
        status == ServiceStatus.ENABLED_NOT_RESPONDING -> Quadruple(
            "Service Not Responding",
            "Accessibility is enabled in Settings but process is inactive. Try restarting the service.",
            WarningAmber,
            WarningAmberContainer
        )
        else -> Quadruple(
            "Accessibility Service Disabled",
            "ReelGuard requires accessibility permission to detect Reels navigation.",
            DangerRed,
            DangerRedContainer
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalSurfaceBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = statusTitle,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusDesc,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            if (status != ServiceStatus.ACTIVE) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onEnableClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentMint,
                        contentColor = Color(0xFF003824)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enable in Android Settings",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MainToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalSurfaceBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Block Instagram Reels",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Automatically redirect away from Instagram Reels while keeping the rest of Instagram available.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF003824),
                    checkedTrackColor = AccentMint,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = CharcoalSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun StatisticsCard(
    blockedCountToday: Int,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalSurfaceBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Reels blocked today",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = blockedCountToday.toString(),
                    color = AccentMint,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "Local counter only • Not shared",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    onEnableAccessibility: () -> Unit,
    onOpenInstagram: () -> Unit,
    onRunDiagnostics: () -> Unit,
    onViewPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "QUICK ACTIONS",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedActionButton(
                label = "Accessibility",
                onClick = onEnableAccessibility,
                modifier = Modifier.weight(1f)
            )
            OutlinedActionButton(
                label = "Open Instagram",
                onClick = onOpenInstagram,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedActionButton(
                label = "Diagnostics",
                onClick = onRunDiagnostics,
                modifier = Modifier.weight(1f)
            )
            OutlinedActionButton(
                label = "Security & Privacy",
                onClick = onViewPrivacy,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun OutlinedActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CharcoalSurface)
            .border(1.dp, CharcoalSurfaceBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PrivacySection(
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalSurfaceBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Privacy Guarantees",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            val items = listOf(
                "Data stays strictly on this device",
                "No internet permission in manifest",
                "No Instagram login or credentials required",
                "Zero analytics, telemetry or trackers",
                "Zero advertising libraries",
                "Never reads messages, comments or captions"
            )

            for (item in items) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "•",
                        color = AccentMint,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = item,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


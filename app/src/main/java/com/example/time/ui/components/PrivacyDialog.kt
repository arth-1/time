package com.example.time.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.time.ui.theme.AccentMint
import com.example.time.ui.theme.CharcoalBackground
import com.example.time.ui.theme.CharcoalSurfaceBorder
import com.example.time.ui.theme.TextMuted
import com.example.time.ui.theme.TextPrimary
import com.example.time.ui.theme.TextSecondary

@Composable
fun PrivacyDialog(
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = CharcoalBackground,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CharcoalSurfaceBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Security & Privacy",
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

                PrivacyPoint(
                    title = "Strict Network Isolation",
                    description = "ReelGuard does not declare android.permission.INTERNET in its AndroidManifest. " +
                            "It is physically incapable of making network calls, sending telemetry, or uploading data."
                )

                PrivacyPoint(
                    title = "Accessibility Data Minimization",
                    description = "The accessibility service inspects only resource IDs and node visibility. " +
                            "It never calls node.getText(), node.getContentDescription(), or inspects input fields. " +
                            "Your messages, comments, search queries, and passwords are never read or touched."
                )

                PrivacyPoint(
                    title = "Package Isolation",
                    description = "ReelGuard's Accessibility Service explicitly filters for 'com.instagram.android'. " +
                            "Events from all other applications (browsers, messaging, banking, email) are dropped immediately without inspection."
                )

                PrivacyPoint(
                    title = "Zero Cloud Backup",
                    description = "Backup configurations explicitly exclude local preferences and diagnostic data from Android cloud backups."
                )

                PrivacyPoint(
                    title = "Zero 3rd-Party Trackers",
                    description = "No Firebase, no crash analytics, no Facebook SDK, no advertising libraries, no external dependencies."
                )
            }
        }
    }
}

@Composable
private fun PrivacyPoint(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "✓",
                color = AccentMint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(start = 22.dp)
        )
    }
}


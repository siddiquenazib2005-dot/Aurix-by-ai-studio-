package com.example.aurix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AurixBorder
import com.example.ui.theme.AurixCardHover
import com.example.ui.theme.AurixCardSurface
import com.example.ui.theme.AurixCyan
import com.example.ui.theme.AurixEmerald
import com.example.ui.theme.AurixTextMuted
import com.example.ui.theme.AurixTextPrimary
import com.example.ui.theme.AurixTextSecondary
import com.example.ui.theme.AurixVoid

@Composable
fun WhyEngineTelemetryDialog(
    userIntent: String,
    interpretation: String,
    planSummary: String,
    toolInvoked: String,
    resultReport: String,
    realityVerification: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = AurixCyan, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AURIX WHY ENGINE TELEMETRY",
                    color = AurixCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TelemetryNode("1. User Intent", userIntent)
                TelemetryNode("2. Neural Interpretation", interpretation)
                TelemetryNode("3. Action Plan", planSummary)
                TelemetryNode("4. Executed Tool", toolInvoked)
                TelemetryNode("5. Device Result", resultReport)

                Card(
                    colors = CardDefaults.cardColors(containerColor = AurixCardHover),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AurixEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Reality Engine Verification", color = AurixEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(realityVerification, color = AurixTextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AurixCyan, contentColor = AurixVoid)
            ) {
                Text("Close Telemetry")
            }
        },
        containerColor = AurixCardSurface
    )
}

@Composable
private fun TelemetryNode(step: String, detail: String) {
    Column {
        Text(text = step, color = AurixTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(text = detail, color = AurixTextPrimary, fontSize = 12.sp)
    }
}

package com.example.aurix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aurix.data.local.entity.MissionEntity
import com.example.aurix.data.model.MissionStatus
import com.example.aurix.data.model.MissionStep
import com.example.aurix.data.model.StepStatus
import com.example.aurix.mission.MissionEngine
import com.example.ui.theme.AurixAmber
import com.example.ui.theme.AurixBorder
import com.example.ui.theme.AurixCardHover
import com.example.ui.theme.AurixCardSurface
import com.example.ui.theme.AurixCrimson
import com.example.ui.theme.AurixCyan
import com.example.ui.theme.AurixEmerald
import com.example.ui.theme.AurixTextMuted
import com.example.ui.theme.AurixTextPrimary
import com.example.ui.theme.AurixTextSecondary
import com.example.ui.theme.AurixVoid

@Composable
fun MissionsScreen(
    missions: List<MissionEntity>,
    missionEngine: MissionEngine,
    onResumeMission: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AurixVoid)
            .padding(16.dp)
    ) {
        Text(
            text = "AGENT MISSION ENGINE",
            color = AurixCyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "Multi-Step Hardware Automation & Resumption",
            color = AurixTextMuted,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (missions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No active missions. Try saying:\n\"Mummy ko message kar aur 8 baje reminder laga dena\"",
                    color = AurixTextMuted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(missions, key = { it.id }) { mission ->
                    val steps = remember(mission.stepsJson) {
                        missionEngine.deserializeSteps(mission.stepsJson)
                    }
                    MissionCard(
                        mission = mission,
                        steps = steps,
                        onResume = { onResumeMission(mission.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MissionCard(
    mission: MissionEntity,
    steps: List<MissionStep>,
    onResume: () -> Unit
) {
    val (statusColor: Color, statusLabel: String) = when (mission.status) {
        MissionStatus.COMPLETED -> Pair(AurixEmerald, "COMPLETED")
        MissionStatus.IN_PROGRESS -> Pair(AurixCyan, "IN PROGRESS")
        MissionStatus.AWAITING_APPROVAL -> Pair(AurixAmber, "AWAITING APPROVAL")
        MissionStatus.FAILED -> Pair(AurixCrimson, "FAILED")
        MissionStatus.PENDING -> Pair(AurixTextMuted, "PENDING")
        MissionStatus.CANCELLED -> Pair(AurixTextMuted, "CANCELLED")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AurixCardSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AurixBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mission.title,
                    color = AurixTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Goal: \"${mission.userGoal}\"",
                color = AurixTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Step breakdown
            steps.forEachIndexed { idx, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (step.status) {
                        StepStatus.SUCCESS -> Icons.Default.CheckCircle
                        StepStatus.FAILED -> Icons.Default.Error
                        StepStatus.EXECUTING, StepStatus.VERIFYING -> Icons.Default.Refresh
                        else -> Icons.Default.Schedule
                    }
                    val iconTint = when (step.status) {
                        StepStatus.SUCCESS -> AurixEmerald
                        StepStatus.FAILED -> AurixCrimson
                        StepStatus.EXECUTING, StepStatus.VERIFYING -> AurixCyan
                        else -> AurixTextMuted
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${idx + 1}. ${step.title}",
                            color = AurixTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (!step.realityVerificationReport.isNullOrBlank()) {
                            Text(
                                text = "Reality: ${step.realityVerificationReport}",
                                color = AurixEmerald,
                                fontSize = 10.sp
                            )
                        } else if (!step.errorReason.isNullOrBlank()) {
                            Text(
                                text = "Error: ${step.errorReason}",
                                color = AurixCrimson,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Resume Button if paused or failed (Phase 8 Mission Resume)
            if (mission.status == MissionStatus.FAILED || mission.status == MissionStatus.PENDING) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = AurixCardHover, contentColor = AurixCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Resume Interrupted Steps", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

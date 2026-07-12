package br.com.cea.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cea.data.CeaDatabaseHelper
import br.com.cea.model.UserProfile
import java.util.Locale

@Composable
fun ProfileScreen(
    modifier: Modifier,
    profile: UserProfile,
    waterMl: Int,
    database: CeaDatabaseHelper,
    onEdit: () -> Unit,
    onWater: () -> Unit,
    onProgress: () -> Unit
) {
    val completedCount = remember(profile) { database.getCompletedWorkoutsCount() }
    val workoutHistory = remember(completedCount) { database.getWorkoutHistoryList() }
    val activeDays = remember(completedCount) { database.getActiveTrainingDaysCount() }
    val streakDays = remember(completedCount) { database.getCurrentWorkoutStreakDays() }
    val mostTrainedObjective = remember(completedCount) { database.getMostTrainedObjective() ?: "-" }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar()
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.name, color = CeaColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Objetivo: ${profile.objective}", color = CeaColors.Muted, fontSize = 10.sp)
            }
            GhostAction("Editar perfil", onEdit)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(completedCount.toString(), "Concluidos", Modifier.weight(1f))
            MetricCard(activeDays.toString(), "Dias ativos", Modifier.weight(1f))
            MetricCard(formatDays(streakDays), "Sequencia", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        MetricCard(mostTrainedObjective, "Mais treinado")
        Spacer(Modifier.height(18.dp))
        CeaCard {
            SectionTitle("Hidratacao")
            Text("$waterMl ml de ${profile.dailyWaterGoalMl} ml", color = CeaColors.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (waterMl.toFloat() / profile.dailyWaterGoalMl.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = CeaColors.Green,
                trackColor = CeaColors.CardAlt
            )
            Spacer(Modifier.height(12.dp))
            PrimaryAction("+ 250 ml", Modifier.fillMaxWidth(), onClick = onWater)
        }
        Spacer(Modifier.height(18.dp))
        SectionTitle("Historico recente")
        Spacer(Modifier.height(8.dp))
        if (workoutHistory.isEmpty()) {
            Text(
                text = "Nenhum treino concluido recentemente.",
                color = CeaColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            workoutHistory.take(5).forEach { entry ->
                val timestamp = entry.completedAt
                val timeLabel = remember(timestamp) {
                    val diff = System.currentTimeMillis() - timestamp
                    val days = (diff / (1000 * 60 * 60 * 24)).toInt()
                    when {
                        days == 0 -> "Hoje"
                        days == 1 -> "Ontem"
                        days < 7 -> "$days dias atras"
                        else -> {
                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            sdf.format(java.util.Date(timestamp))
                        }
                    }
                }
                WorkoutRow(entry.title, timeLabel, "OK", onProgress)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun formatDays(days: Int): String {
    return if (days == 1) "1 dia" else "$days dias"
}

package br.com.cea.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cea.model.ScheduledWorkout

@Composable
fun HomeScreen(
    modifier: Modifier,
    nextScheduledWorkout: ScheduledWorkout?,
    waterMl: Int,
    weekWorkoutCounts: List<Int>,
    onCreateWorkout: () -> Unit,
    onMyWorkouts: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    onWater: () -> Unit,
    onProgress: () -> Unit
) {
    val weeklyCompleted = weekWorkoutCounts.sum()

    Column(modifier) {
        CeaCard {
            SectionTitle("Proximo treino")
            Spacer(Modifier.height(8.dp))
            if (nextScheduledWorkout == null) {
                Text("Nenhum treino agendado.", color = CeaColors.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                PrimaryAction("Ver treinos", Modifier.fillMaxWidth(), onClick = onMyWorkouts)
            } else {
                Text(
                    nextScheduledWorkout.workoutTitle,
                    color = CeaColors.Text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    nextScheduledWorkout.workoutObjective,
                    color = CeaColors.Muted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                PrimaryAction("Iniciar treino", Modifier.fillMaxWidth()) {
                    onStartWorkout(nextScheduledWorkout.workoutId)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CeaCard(Modifier.weight(1f)) {
                Text("$waterMl ml", color = CeaColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Agua hoje", color = CeaColors.Muted, fontSize = 10.sp)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (waterMl / 2500f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = CeaColors.Green,
                    trackColor = CeaColors.CardAlt
                )
                Spacer(Modifier.height(8.dp))
                GhostAction("+ 250 ml", onWater)
            }

            CeaCard(Modifier.weight(1f)) {
                Text(weeklyCompleted.toString(), color = CeaColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Treinos na semana", color = CeaColors.Muted, fontSize = 10.sp)
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    weekWorkoutCounts.forEach { count ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(if (count > 0) 30.dp else 8.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (count > 0) CeaColors.Green else CeaColors.CardAlt)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                GhostAction("Progresso", onProgress)
            }
        }

        Spacer(Modifier.height(14.dp))
        CeaCard {
            SectionTitle("Treinos")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryAction("Criar treino", Modifier.weight(1f), onClick = onCreateWorkout)
                GhostAction("Meus treinos", onMyWorkouts)
            }
        }
    }
}

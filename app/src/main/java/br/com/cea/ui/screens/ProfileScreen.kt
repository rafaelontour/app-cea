package br.com.cea.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cea.data.CeaDatabaseHelper
import br.com.cea.model.DayState
import br.com.cea.model.UserProfile
import java.util.Calendar
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
            MetricCard((18 + completedCount).toString(), "Concluidos", Modifier.weight(1f))
            MetricCard("24", "Dias ativos", Modifier.weight(1f))
            MetricCard("12h40", "Tempo total", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("7 dias", "Sequencia", Modifier.weight(1f))
            MetricCard(profile.objective, "Mais treinado", Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        ProfileCalendarCard()
        Spacer(Modifier.height(18.dp))
        CeaCard {
            SectionTitle("Hidratacao")
            Text("$waterMl ml de 2500 ml", color = CeaColors.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (waterMl / 2500f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = CeaColors.Green,
                trackColor = CeaColors.CardAlt
            )
            Spacer(Modifier.height(12.dp))
            PrimaryAction("+ 250 ml", Modifier.fillMaxWidth(), onWater)
        }
        Spacer(Modifier.height(18.dp))
        SectionTitle("Historico recente")
        Spacer(Modifier.height(8.dp))
        if (workoutHistory.isEmpty()) {
            Text(
                text = "Nenhum treino concluído recentemente.",
                color = CeaColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            workoutHistory.take(5).forEach { (title, timestamp) ->
                val timeLabel = remember(timestamp) {
                    val diff = System.currentTimeMillis() - timestamp
                    val days = (diff / (1000 * 60 * 60 * 24)).toInt()
                    when {
                        days == 0 -> "Hoje"
                        days == 1 -> "Ontem"
                        days < 7 -> "$days dias atrás"
                        else -> {
                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            sdf.format(java.util.Date(timestamp))
                        }
                    }
                }
                WorkoutRow(title, "$timeLabel - 30 min", "OK", onProgress)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ProfileCalendarCard() {
    var currentMonthOffset by remember { mutableIntStateOf(0) }

    val calendar = remember(currentMonthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JUNE)
            add(Calendar.MONTH, currentMonthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val monthName = remember(calendar) {
        calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR"))
            ?.replaceFirstChar { it.uppercase() } ?: ""
    }
    val year = remember(calendar) { calendar.get(Calendar.YEAR) }
    val totalDays = remember(calendar) { calendar.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val firstDayOfWeek = remember(calendar) { calendar.get(Calendar.DAY_OF_WEEK) }

    val startOffset = remember(firstDayOfWeek) {
        when (firstDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    val calendarCells = remember(startOffset, totalDays) {
        buildList {
            repeat(startOffset) { add(null) }
            for (day in 1..totalDays) { add(day) }
        }
    }

    CeaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionTitle("Frequencia de treinos")
                Spacer(Modifier.height(2.dp))
                Text("$monthName $year", color = CeaColors.Muted, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "<",
                    color = CeaColors.Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable { currentMonthOffset-- }
                        .padding(8.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = ">",
                    color = CeaColors.Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable { currentMonthOffset++ }
                        .padding(8.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("S", "T", "Q", "Q", "S", "S", "D").forEach {
                Text(
                    text = it,
                    color = CeaColors.Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        calendarCells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    if (day != null) {
                        val state = when (day) {
                            4, 10, 17, 20 -> DayState.Completed
                            8 -> DayState.Missed
                            12, 24 -> DayState.Scheduled
                            else -> DayState.Empty
                        }
                        CalendarCell(day, state)
                    } else {
                        Spacer(Modifier.size(36.dp))
                    }
                }
                if (week.size < 7) {
                    repeat(7 - week.size) {
                        Spacer(Modifier.size(36.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("Concluido", CeaColors.Green)
            StatusPill("Agendado", CeaColors.Blue)
            StatusPill("Perdido", CeaColors.Red)
        }
    }
}

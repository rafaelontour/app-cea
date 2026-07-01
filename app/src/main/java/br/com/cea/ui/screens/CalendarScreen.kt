package br.com.cea.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cea.data.CeaDatabaseHelper
import br.com.cea.model.DayState
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    modifier: Modifier,
    database: CeaDatabaseHelper,
    onStart: () -> Unit
) {
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

    val completedDays = remember(database, currentMonthOffset, year) {
        database.getCompletedDaysInMonth(year, calendar.get(Calendar.MONTH))
    }

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "<",
                color = CeaColors.Green,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable { currentMonthOffset-- }
                    .padding(8.dp)
            )
            Text(
                text = "$monthName $year",
                color = CeaColors.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
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
                        val state = when {
                            day in completedDays -> DayState.Completed
                            day == 8 -> DayState.Missed
                            day in listOf(12, 24) -> DayState.Scheduled
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
        val workouts = remember(database) { database.listWorkouts(publicOnly = false) }
        val activePlanned = workouts.firstOrNull()
        if (activePlanned != null) {
            Spacer(Modifier.height(16.dp))
            SectionTitle("Treino Agendado")
            Spacer(Modifier.height(6.dp))
            WorkoutRow(
                title = activePlanned.title,
                subtitle = "${activePlanned.objective} - ${activePlanned.duration}",
                action = "Iniciar",
                onStart = onStart
            )
        } else {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Nenhum treino agendado ou criado hoje.",
                color = CeaColors.Muted,
                fontSize = 11.sp
            )
        }
    }
}

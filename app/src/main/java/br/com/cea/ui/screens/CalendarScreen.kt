package br.com.cea.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onStart: (Long) -> Unit
) {
    var currentMonthOffset by remember { mutableIntStateOf(0) }
    var selectedDay by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
    var refreshSchedule by remember { mutableIntStateOf(0) }

    val calendar = remember(currentMonthOffset) {
        Calendar.getInstance().apply {
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
    val selectedDayInMonth = selectedDay.coerceIn(1, totalDays)
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

    val completedDays = remember(database, currentMonthOffset, year, refreshSchedule) {
        database.getCompletedDaysInMonth(year, calendar.get(Calendar.MONTH))
    }
    val scheduledDays = remember(database, currentMonthOffset, year, refreshSchedule) {
        database.getScheduledDaysInMonth(year, calendar.get(Calendar.MONTH))
    }
    val missedDays = remember(database, currentMonthOffset, year, refreshSchedule) {
        database.getMissedScheduledDaysInMonth(year, calendar.get(Calendar.MONTH))
    }
    val workouts = remember(database, refreshSchedule) { database.listWorkouts(publicOnly = false) }
    val upcoming = remember(database, refreshSchedule) { database.getUpcomingScheduledWorkouts() }
    val selectedDate = remember(year, calendar, selectedDayInMonth) {
        selectedDateMillis(year, calendar.get(Calendar.MONTH), selectedDayInMonth)
    }
    val scheduledWorkoutIdsForSelectedDay = remember(database, refreshSchedule, selectedDate) {
        database.getScheduledWorkoutIdsForDay(selectedDate)
    }
    val selectedDayWorkouts = remember(database, refreshSchedule, selectedDate) {
        database.getScheduledWorkoutsForDay(selectedDate)
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
                            day in missedDays -> DayState.Missed
                            day in scheduledDays -> DayState.Scheduled
                            else -> DayState.Empty
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (day == selectedDayInMonth) 2.dp else 0.dp,
                                    color = if (day == selectedDayInMonth) CeaColors.Green else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedDay = day },
                            contentAlignment = Alignment.Center
                        ) {
                            CalendarCell(day, state)
                        }
                    } else {
                        Spacer(Modifier.size(40.dp))
                    }
                }
                if (week.size < 7) {
                    repeat(7 - week.size) {
                        Spacer(Modifier.size(40.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("Concluído", CeaColors.Green)
            StatusPill("Agendado", CeaColors.Blue)
            StatusPill("Perdido", CeaColors.Red)
        }
        Spacer(Modifier.height(16.dp))
        SectionTitle("Agendar treino")
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Dia selecionado: ${selectedDayInMonth.toString().padStart(2, '0')}/${
                (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            }/$year",
            color = CeaColors.Muted,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(8.dp))
        SectionTitle("Treinos deste dia")
        Spacer(Modifier.height(8.dp))
        if (selectedDayWorkouts.isEmpty()) {
            Text(
                text = "Nenhum treino agendado para este dia.",
                color = CeaColors.Muted,
                fontSize = 11.sp
            )
        } else {
            selectedDayWorkouts.forEach { scheduled ->
                WorkoutRow(
                    title = scheduled.workoutTitle,
                    subtitle = "${scheduled.workoutObjective} - ${scheduled.workoutDuration}",
                    action = "Iniciar",
                    onStart = { onStart(scheduled.workoutId) },
                    extraActions = listOf(
                        "Cancelar" to {
                            database.cancelScheduledWorkout(scheduled.id)
                            refreshSchedule++
                        }
                    )
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        if (workouts.isEmpty()) {
            Text(
                text = "Crie ou importe um treino para poder agendar.",
                color = CeaColors.Muted,
                fontSize = 11.sp
            )
        } else {
            workouts.take(4).forEach { workout ->
                val alreadyScheduled = workout.id in scheduledWorkoutIdsForSelectedDay
                WorkoutRow(
                    title = workout.title,
                    subtitle = "${workout.objective} - ${workout.duration}",
                    action = if (alreadyScheduled) "Agendado" else "Agendar",
                    onStart = {
                        if (!alreadyScheduled) {
                            database.scheduleWorkout(workout.id, selectedDate)
                            refreshSchedule++
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionTitle("Próximos agendados")
        Spacer(Modifier.height(6.dp))
        if (upcoming.isEmpty()) {
            Text(
                text = "Nenhum treino agendado.",
                color = CeaColors.Muted,
                fontSize = 11.sp
            )
        } else {
            upcoming.forEach { scheduled ->
                WorkoutRow(
                    title = scheduled.workoutTitle,
                    subtitle = "${formatScheduleDate(scheduled.scheduledAt)} - ${scheduled.workoutObjective} - ${scheduled.workoutDuration}",
                    action = "Iniciar",
                    onStart = { onStart(scheduled.workoutId) },
                    extraActions = listOf(
                        "Reagendar" to {
                            database.rescheduleWorkout(scheduled.id, selectedDate)
                            refreshSchedule++
                        },
                        "Cancelar" to {
                            database.cancelScheduledWorkout(scheduled.id)
                            refreshSchedule++
                        }
                    )
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

private fun selectedDateMillis(year: Int, month: Int, day: Int): Long {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatScheduleDate(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')}/${
        (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    }"
}

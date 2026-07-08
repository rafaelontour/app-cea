package br.com.cea.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import br.com.cea.model.Workout
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    modifier: Modifier,
    database: CeaDatabaseHelper,
    onStart: (Long) -> Unit
) {
    var currentMonthOffset by remember { mutableIntStateOf(0) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var refreshSchedule by remember { mutableIntStateOf(0) }
    var workoutWaitingForDate by remember { mutableStateOf<Workout?>(null) }

    val calendar = remember(currentMonthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, currentMonthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val month = calendar.get(Calendar.MONTH)
    val year = calendar.get(Calendar.YEAR)
    val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val selectedDayInMonth = selectedDay?.coerceIn(1, totalDays)
    val selectedDate = remember(year, month, selectedDayInMonth) {
        selectedDayInMonth?.let { selectedDateMillis(year, month, it) }
    }

    val monthName = remember(calendar) {
        calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR"))
            ?.replaceFirstChar { it.uppercase() } ?: ""
    }
    val calendarCells = remember(calendar) {
        val startOffset = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        buildList {
            repeat(startOffset) { add(null) }
            for (day in 1..totalDays) add(day)
        }
    }

    val completedDays = remember(database, currentMonthOffset, year, refreshSchedule) {
        database.getCompletedDaysInMonth(year, month)
    }
    val scheduledDays = remember(database, currentMonthOffset, year, refreshSchedule) {
        database.getScheduledDaysInMonth(year, month)
    }
    val missedDays = remember(database, currentMonthOffset, year, refreshSchedule) {
        database.getMissedScheduledDaysInMonth(year, month)
    }
    val workouts = remember(database, refreshSchedule) { database.listWorkouts(publicOnly = false) }
    val createdWorkouts = remember(workouts) { workouts.filter { !it.publicWorkout } }
    val presetWorkouts = remember(workouts) { workouts.filter { it.publicWorkout } }
    val upcoming = remember(database, refreshSchedule) { database.getUpcomingScheduledWorkouts() }
    val scheduledWorkoutIdsForSelectedDay = remember(database, refreshSchedule, selectedDate) {
        selectedDate?.let { database.getScheduledWorkoutIdsForDay(it) }.orEmpty()
    }
    val selectedDayWorkouts = remember(database, refreshSchedule, selectedDate) {
        selectedDate?.let { database.getScheduledWorkoutsForDay(it) }.orEmpty()
    }

    fun scheduleWorkout(workout: Workout, date: Long) {
        database.scheduleWorkout(workout.id, date)
        refreshSchedule++
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
                modifier = Modifier.clickable { currentMonthOffset-- }.padding(8.dp)
            )
            Text("$monthName $year", color = CeaColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                text = ">",
                color = CeaColors.Green,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.clickable { currentMonthOffset++ }.padding(8.dp)
            )
        }

        Spacer(Modifier.height(10.dp))
        CalendarWeekHeader(cellSize = 40)
        Spacer(Modifier.height(6.dp))
        calendarCells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.size(40.dp))
                    } else {
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
                    }
                }
                if (week.size < 7) repeat(7 - week.size) { Spacer(Modifier.size(40.dp)) }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("Concluido", CeaColors.Green)
            StatusPill("Agendado", CeaColors.Blue)
            StatusPill("Perdido", CeaColors.Red)
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Agendar treino")
        Spacer(Modifier.height(6.dp))
        Text(
            text = selectedDate?.let { "Dia selecionado: ${formatFullDate(it)}" } ?: "Nenhum dia selecionado",
            color = CeaColors.Muted,
            fontSize = 11.sp
        )

        if (selectedDate != null) {
            Spacer(Modifier.height(14.dp))
            SectionTitle("Treinos deste dia")
            Spacer(Modifier.height(8.dp))
            if (selectedDayWorkouts.isEmpty()) {
                Text("Nenhum treino agendado para este dia.", color = CeaColors.Muted, fontSize = 11.sp)
            } else {
                selectedDayWorkouts.forEach { scheduled ->
                    WorkoutRow(
                        title = scheduled.workoutTitle,
                        subtitle = scheduled.workoutObjective,
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
        }

        Spacer(Modifier.height(16.dp))
        WorkoutScheduleSection(
            title = "Treinos criados",
            emptyText = "Nenhum treino criado para agendar.",
            workouts = createdWorkouts,
            scheduledWorkoutIds = scheduledWorkoutIdsForSelectedDay,
            selectedDate = selectedDate,
            onSchedule = ::scheduleWorkout,
            onNeedsDate = { workoutWaitingForDate = it }
        )

        Spacer(Modifier.height(14.dp))
        WorkoutScheduleSection(
            title = "Treinos predefinidos",
            emptyText = "Nenhum treino predefinido disponivel.",
            workouts = presetWorkouts,
            scheduledWorkoutIds = scheduledWorkoutIdsForSelectedDay,
            selectedDate = selectedDate,
            onSchedule = ::scheduleWorkout,
            onNeedsDate = { workoutWaitingForDate = it }
        )

        Spacer(Modifier.height(16.dp))
        SectionTitle("Proximos agendados")
        Spacer(Modifier.height(8.dp))
        if (upcoming.isEmpty()) {
            Text("Nenhum treino agendado.", color = CeaColors.Muted, fontSize = 11.sp)
        } else {
            upcoming.forEach { scheduled ->
                WorkoutRow(
                    title = scheduled.workoutTitle,
                    subtitle = "${formatShortDate(scheduled.scheduledAt)} - ${scheduled.workoutObjective}",
                    action = "Iniciar",
                    onStart = { onStart(scheduled.workoutId) },
                    extraActions = listOf(
                        "Reagendar" to {
                            if (selectedDate != null) {
                                database.rescheduleWorkout(scheduled.id, selectedDate)
                                refreshSchedule++
                            }
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

    val pendingWorkout = workoutWaitingForDate
    if (pendingWorkout != null) {
        ScheduleDateDialog(
            workoutTitle = pendingWorkout.title,
            onDismiss = { workoutWaitingForDate = null },
            onConfirm = { date ->
                scheduleWorkout(pendingWorkout, date)
                workoutWaitingForDate = null
            }
        )
    }
}

@Composable
private fun WorkoutScheduleSection(
    title: String,
    emptyText: String,
    workouts: List<Workout>,
    scheduledWorkoutIds: Set<Long>,
    selectedDate: Long?,
    onSchedule: (Workout, Long) -> Unit,
    onNeedsDate: (Workout) -> Unit
) {
    SectionTitle(title)
    Spacer(Modifier.height(8.dp))
    if (workouts.isEmpty()) {
        Text(emptyText, color = CeaColors.Muted, fontSize = 11.sp)
    } else {
        workouts.forEach { workout ->
            val alreadyScheduled = selectedDate != null && workout.id in scheduledWorkoutIds
            WorkoutRow(
                title = workout.title,
                subtitle = workout.objective,
                action = if (alreadyScheduled) "Agendado" else "Agendar",
                actionDone = alreadyScheduled,
                onStart = {
                    if (!alreadyScheduled) {
                        if (selectedDate != null) onSchedule(workout, selectedDate) else onNeedsDate(workout)
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ScheduleDateDialog(
    workoutTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var monthOffset by remember { mutableIntStateOf(0) }
    var pickedDay by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
    val pickerCalendar = remember(monthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val month = pickerCalendar.get(Calendar.MONTH)
    val year = pickerCalendar.get(Calendar.YEAR)
    val totalDays = pickerCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthName = pickerCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR"))
        ?.replaceFirstChar { it.uppercase() } ?: ""
    val cells = remember(pickerCalendar) {
        val startOffset = when (pickerCalendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        buildList {
            repeat(startOffset) { add(null) }
            for (day in 1..totalDays) add(day)
        }
    }

    LaunchedEffect(totalDays) {
        pickedDay = pickedDay.coerceIn(1, totalDays)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escolher data", color = CeaColors.Text, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(workoutTitle, color = CeaColors.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GhostAction("<") { monthOffset-- }
                    Text("$monthName $year", color = CeaColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    GhostAction(">") { monthOffset++ }
                }
                Spacer(Modifier.height(8.dp))
                CalendarWeekHeader(cellSize = 32)
                Spacer(Modifier.height(6.dp))
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        week.forEach { day ->
                            if (day == null) {
                                Spacer(Modifier.size(32.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (day == pickedDay) 2.dp else 0.dp,
                                            color = if (day == pickedDay) CeaColors.Green else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { pickedDay = day },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(day.toString(), color = CeaColors.Text, fontSize = 11.sp)
                                }
                            }
                        }
                        if (week.size < 7) repeat(7 - week.size) { Spacer(Modifier.size(32.dp)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedDateMillis(year, month, pickedDay)) }) {
                Text("Agendar", color = CeaColors.Green, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = CeaColors.Text, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CeaColors.Card,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun CalendarWeekHeader(cellSize: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("S", "T", "Q", "Q", "S", "S", "D").forEach {
            Text(
                text = it,
                color = CeaColors.Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(cellSize.dp),
                textAlign = TextAlign.Center
            )
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

private fun formatShortDate(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')}/${
        (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    }"
}

private fun formatFullDate(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "${formatShortDate(timestamp)}/${cal.get(Calendar.YEAR)}"
}

package br.com.cea.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cea.data.CeaDatabaseHelper
import br.com.cea.model.Workout

@Composable
fun ActiveWorkoutScreen(
    modifier: Modifier,
    workout: Workout,
    database: CeaDatabaseHelper,
    onFinished: () -> Unit
) {
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var isResting by remember { mutableStateOf(false) }
    var restTimeInput by remember { mutableStateOf("15") }
    var customRestSeconds by remember { mutableIntStateOf(15) }
    var workoutCompleted by remember { mutableStateOf(false) }
    val startedAt = remember { System.currentTimeMillis() }

    val exerciseSpecs = remember(workout) {
        workout.exerciseSpecs.ifEmpty {
            workout.exercises.map { br.com.cea.model.WorkoutExerciseSpec(name = it) }
        }
    }
    val exercises = exerciseSpecs.map { it.name }
    val currentSpec = exerciseSpecs.getOrNull(currentExerciseIndex)
    val currentExercise = currentSpec?.name ?: ""

    val exercisesCatalog = remember { database.listExercises() }
    val cleanExerciseName = remember(currentExercise) {
        currentExercise.split("-").first().trim()
    }
    val currentExerciseObj = remember(cleanExerciseName, exercisesCatalog) {
        exercisesCatalog.find { it.name.equals(cleanExerciseName, ignoreCase = true) }
    }

    val defaultWorkSeconds = remember(currentExerciseObj) {
        when (currentExerciseObj?.level) {
            "Iniciante" -> 20
            "Intermediário", "Intermediario" -> 25
            "Avançado", "Avancado" -> 30
            else -> 30
        }
    }

    LaunchedEffect(currentSpec?.restSeconds) {
        val rest = currentSpec?.restSeconds?.takeIf { it > 0 } ?: 15
        restTimeInput = rest.toString()
        customRestSeconds = rest
    }

    var timeLeft by remember(currentExerciseIndex, isResting, defaultWorkSeconds) {
        val workSeconds = currentSpec?.durationSeconds?.takeIf { it > 0 } ?: defaultWorkSeconds
        mutableIntStateOf(if (isResting) customRestSeconds else workSeconds)
    }
    var isTimerRunning by remember { mutableStateOf(true) }

    fun finishWorkout() {
        if (!workoutCompleted) {
            val elapsedSeconds = ((System.currentTimeMillis() - startedAt) / 1000L).toInt()
            database.logWorkoutCompletion(workout.id, elapsedSeconds)
            isTimerRunning = false
            workoutCompleted = true
        }
    }

    LaunchedEffect(isTimerRunning, timeLeft) {
        if (workoutCompleted) {
            return@LaunchedEffect
        }
        if (isTimerRunning && timeLeft > 0) {
            kotlinx.coroutines.delay(1000L)
            timeLeft--
        } else if (timeLeft == 0) {
            if (!isResting) {
                isResting = true
            } else {
                isResting = false
                if (currentExerciseIndex < exercises.size - 1) {
                    currentExerciseIndex++
                } else {
                    finishWorkout()
                }
            }
        }
    }

    Column(modifier) {
        Text(workout.title, color = CeaColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("${exercises.size} Exercícios em sequência", color = CeaColors.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        CeaCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isResting) "DESCANSO" else "EXECUTANDO",
                    color = if (isResting) CeaColors.Blue else CeaColors.Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                if (isResting) {
                    val nextExerciseName = exercises.getOrNull(currentExerciseIndex + 1) ?: "Fim"
                    Text(
                        text = "Próximo: $nextExerciseName",
                        color = CeaColors.Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    val nextExerciseObj = remember(nextExerciseName, exercisesCatalog) {
                        val clean = nextExerciseName.split("-").first().trim()
                        exercisesCatalog.find { it.name.equals(clean, ignoreCase = true) }
                    }
                    if (nextExerciseObj != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(nextExerciseObj.level, CeaColors.Green)
                            StatusPill(nextExerciseObj.muscleGroup, CeaColors.Blue)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Descrição / Instruções do próximo:",
                            color = CeaColors.Text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(Modifier.height(4.dp))
                        nextExerciseObj.instructions.split("\n").filter { it.isNotBlank() }.forEachIndexed { idx, step ->
                            Text(
                                text = "${idx + 1}. $step",
                                color = CeaColors.Muted,
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Start),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                } else {
                    Text(
                        text = currentExercise,
                        color = CeaColors.Text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (currentSpec != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${currentSpec.sets} séries - ${currentSpec.reps} reps - ${currentSpec.restSeconds}s descanso",
                            color = CeaColors.Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (currentExerciseObj != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(currentExerciseObj.level, CeaColors.Green)
                            StatusPill(currentExerciseObj.muscleGroup, CeaColors.Blue)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Descrição / Instruções:",
                            color = CeaColors.Text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(Modifier.height(4.dp))
                        currentExerciseObj.instructions.split("\n").filter { it.isNotBlank() }.forEachIndexed { idx, step ->
                            Text(
                                text = "${idx + 1}. $step",
                                color = CeaColors.Muted,
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Start),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "${timeLeft}s",
                    color = CeaColors.Text,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { isTimerRunning = !isTimerRunning },
                        colors = ButtonDefaults.buttonColors(containerColor = CeaColors.CardAlt),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isTimerRunning) "Pausar" else "Retomar", color = CeaColors.Text)
                    }
                    if (!isResting) {
                        Button(
                            onClick = { timeLeft += 5 },
                            colors = ButtonDefaults.buttonColors(containerColor = CeaColors.CardAlt),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+5s", color = CeaColors.Green, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = {
                            if (!isResting) {
                                isResting = true
                            } else {
                                isResting = false
                                if (currentExerciseIndex < exercises.size - 1) {
                                    currentExerciseIndex++
                                } else {
                                    finishWorkout()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CeaColors.CardAlt),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Pular", color = CeaColors.Text)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        CeaCard {
            SectionTitle("Configurar tempo de descanso")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CeaInput(
                    label = "Tempo de descanso (segundos)",
                    value = restTimeInput,
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        restTimeInput = it
                        it.toIntOrNull()?.let { s ->
                            if (s > 0) {
                                customRestSeconds = s
                            }
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionTitle("Exercícios do Treino")
        Spacer(Modifier.height(8.dp))
        exercises.forEachIndexed { index, name ->
            val isCurrent = index == currentExerciseIndex && !isResting
            val spec = exerciseSpecs[index]
            val isCompleted = index < currentExerciseIndex

            CeaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .border(
                        width = if (isCurrent) 1.5.dp else 0.dp,
                        color = if (isCurrent) CeaColors.Green else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${index + 1}",
                            color = if (isCompleted) CeaColors.Green else CeaColors.Muted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = name,
                                color = if (isCurrent) CeaColors.Green else if (isCompleted) CeaColors.Muted else CeaColors.Text,
                                fontSize = 14.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "${spec.sets}x ${spec.reps} reps - ${spec.restSeconds}s descanso",
                                color = CeaColors.Muted,
                                fontSize = 10.sp
                            )
                        }
                    }
                    if (isCompleted) {
                        Text("✓ Concluído", color = CeaColors.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else if (isCurrent) {
                        Text("Executando...", color = CeaColors.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Aguardando", color = CeaColors.Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (workoutCompleted) {
        WorkoutFinishedDialog(
            workoutTitle = workout.title,
            exerciseCount = exercises.size,
            onClose = onFinished
        )
    }
}

@Composable
private fun WorkoutFinishedDialog(
    workoutTitle: String,
    exerciseCount: Int,
    onClose: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "finish-pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "finish-scale"
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = {},
        title = {
            Text("Treino concluído", color = CeaColors.Text, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier
                        .size(76.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale),
                    color = CeaColors.Green,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✓", color = Color.Black, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = workoutTitle,
                    color = CeaColors.Text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "$exerciseCount exercícios finalizados. Bom trabalho!",
                    color = CeaColors.Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = CeaColors.Green, contentColor = Color.Black),
                shape = RoundedCornerShape(9.dp)
            ) {
                Text("Ver meus treinos", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CeaColors.Card,
        shape = RoundedCornerShape(16.dp)
    )
}

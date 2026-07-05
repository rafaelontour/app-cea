package br.com.cea.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cea.data.CeaDatabaseHelper
import br.com.cea.model.Exercise
import br.com.cea.model.UserProfile
import br.com.cea.model.Workout
import br.com.cea.model.WorkoutExerciseSpec

@Composable
fun CreateWorkoutScreen(
    modifier: Modifier,
    profile: UserProfile,
    editingWorkoutId: Long?,
    database: CeaDatabaseHelper,
    selectedExercises: MutableList<Exercise>,
    onExercises: (String) -> Unit,
    onSave: (Workout) -> Unit
) {
    val editingWorkout = remember(editingWorkoutId) {
        editingWorkoutId?.let { database.getWorkout(it) }
    }

    var name by remember(editingWorkout) { mutableStateOf(editingWorkout?.title ?: "") }
    var objective by remember(editingWorkout) { mutableStateOf(editingWorkout?.objective ?: profile.objective.ifBlank { "Hipertrofia" }) }
    var level by remember(editingWorkout) { mutableStateOf((editingWorkout?.level ?: profile.level).normalizedTrainingLevel()) }
    var duration by remember(editingWorkout) { mutableStateOf(editingWorkout?.duration ?: "30 min") }
    var muscle by remember { mutableStateOf("Peito") }
    var exerciseToRemove by remember { mutableStateOf<Exercise?>(null) }
    var showLevelIncreaseDialog by remember { mutableStateOf(false) }
    val prescriptionInputs = remember(editingWorkout) {
        mutableStateMapOf<String, ExercisePrescriptionInput>().apply {
            editingWorkout?.exerciseSpecs?.forEach { spec ->
                put(
                    spec.name,
                    ExercisePrescriptionInput(
                        sets = spec.sets.toString(),
                        reps = spec.reps,
                        restSeconds = spec.restSeconds.toString()
                    )
                )
            }
        }
    }

    selectedExercises.forEach { exercise ->
        prescriptionInputs.getOrPut(exercise.name) { ExercisePrescriptionInput() }
    }

    fun buildWorkout(): Workout {
        val specs = selectedExercises.map { exercise ->
            val input = prescriptionInputs[exercise.name] ?: ExercisePrescriptionInput()
            WorkoutExerciseSpec(
                name = exercise.name,
                sets = input.sets.toIntOrNull()?.coerceAtLeast(1) ?: 3,
                reps = input.reps.ifBlank { "10" },
                restSeconds = input.restSeconds.toIntOrNull()?.coerceAtLeast(0) ?: 60
            )
        }
        return Workout(
            title = name.ifBlank { "Meu Treino" },
            objective = objective,
            level = level.normalizedTrainingLevel(),
            duration = duration,
            publicWorkout = false,
            exercises = specs.map { it.name },
            exerciseSpecs = specs
        )
    }

    Column(modifier) {
        CeaInput("Nome do treino", name, onValueChange = { name = it })
        Spacer(Modifier.height(14.dp))
        FieldLabel("Objetivo")
        SelectableChips(listOf("Hipertrofia", "Força", "Cardio"), objective) { objective = it }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Nível")
        SelectableChips(listOf("Iniciante", "Intermediário", "Avançado"), level) { level = it }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Duração")
        SelectableChips(listOf("30 min", "45 min", "60 min"), duration) { duration = it }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Grupo muscular")
        SelectableChips(listOf("Peito", "Costas", "Pernas", "Ombros", "Braços", "Abdominais"), muscle) { muscle = it }
        Spacer(Modifier.height(16.dp))
        PrimaryAction("+ Adicionar exercício", Modifier.fillMaxWidth()) { onExercises(muscle) }
        Spacer(Modifier.height(18.dp))
        SectionTitle("Exercícios selecionados")
        Spacer(Modifier.height(8.dp))
        if (selectedExercises.isEmpty()) {
            CeaCard {
                Text(
                    text = "Nenhum exercício adicionado.",
                    color = CeaColors.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Escolha um grupo muscular e toque em adicionar para montar o treino.",
                    color = CeaColors.Muted,
                    fontSize = 12.sp
                )
            }
        } else {
            selectedExercises.forEachIndexed { index, exercise ->
                SelectedExerciseRow(
                    index = index + 1,
                    exercise = exercise,
                    canMoveUp = index > 0,
                    canMoveDown = index < selectedExercises.lastIndex,
                    onMoveUp = {
                        selectedExercises.removeAt(index)
                        selectedExercises.add(index - 1, exercise)
                    },
                    onMoveDown = {
                        selectedExercises.removeAt(index)
                        selectedExercises.add(index + 1, exercise)
                    },
                    onRemove = { exerciseToRemove = exercise },
                    prescription = prescriptionInputs.getOrPut(exercise.name) { ExercisePrescriptionInput() },
                    onPrescriptionChange = { prescriptionInputs[exercise.name] = it }
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (selectedExercises.isEmpty()) {
            Text(
                text = "Adicione pelo menos um exercício para salvar.",
                color = CeaColors.Red,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        PrimaryAction(
            text = "Salvar treino",
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedExercises.isNotEmpty()
        ) {
            if (isTrainingLevelAbove(level, profile.level)) {
                showLevelIncreaseDialog = true
            } else {
                onSave(buildWorkout())
            }
        }
    }

    val pendingRemoval = exerciseToRemove
    if (pendingRemoval != null) {
        AlertDialog(
            onDismissRequest = { exerciseToRemove = null },
            title = {
                Text("Remover exercício", color = CeaColors.Text, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Remover \"${pendingRemoval.name}\" deste treino?", color = CeaColors.Muted)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedExercises.removeAll { it.name == pendingRemoval.name }
                        prescriptionInputs.remove(pendingRemoval.name)
                        exerciseToRemove = null
                    }
                ) {
                    Text("Remover", color = CeaColors.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToRemove = null }) {
                    Text("Cancelar", color = CeaColors.Text, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CeaColors.Card
        )
    }

    if (showLevelIncreaseDialog) {
        val newLevel = level.normalizedTrainingLevel()
        AlertDialog(
            onDismissRequest = { showLevelIncreaseDialog = false },
            title = {
                Text("Aumentar nível da conta", color = CeaColors.Text, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Este treino está em nível $newLevel, acima do seu nível atual (${profile.level.normalizedTrainingLevel()}). Ao salvar, o nível da sua conta será atualizado para $newLevel.",
                    color = CeaColors.Muted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLevelIncreaseDialog = false
                        onSave(buildWorkout())
                    }
                ) {
                    Text("Salvar e atualizar", color = CeaColors.Green, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLevelIncreaseDialog = false }) {
                    Text("Cancelar", color = CeaColors.Text, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CeaColors.Card
        )
    }
}

private data class ExercisePrescriptionInput(
    val sets: String = "3",
    val reps: String = "10",
    val restSeconds: String = "60"
)

@Composable
private fun SelectedExerciseRow(
    index: Int,
    exercise: Exercise,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    prescription: ExercisePrescriptionInput,
    onPrescriptionChange: (ExercisePrescriptionInput) -> Unit
) {
    CeaCard(modifier = Modifier.padding(bottom = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = index.toString(),
                color = CeaColors.Green,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(exercise.name, color = CeaColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("${exercise.muscleGroup} - ${exercise.level}", color = CeaColors.Muted, fontSize = 10.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onMoveUp, enabled = canMoveUp, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("↑", color = if (canMoveUp) CeaColors.Green else CeaColors.Muted, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onMoveDown, enabled = canMoveDown, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("↓", color = if (canMoveDown) CeaColors.Green else CeaColors.Muted, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onRemove, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("Remover", color = CeaColors.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CeaInput(
                label = "Séries",
                value = prescription.sets,
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
                onValueChange = { onPrescriptionChange(prescription.copy(sets = it)) }
            )
            CeaInput(
                label = "Reps",
                value = prescription.reps,
                modifier = Modifier.weight(1f),
                onValueChange = { onPrescriptionChange(prescription.copy(reps = it)) }
            )
            CeaInput(
                label = "Descanso",
                value = prescription.restSeconds,
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
                onValueChange = { onPrescriptionChange(prescription.copy(restSeconds = it)) }
            )
        }
    }
}

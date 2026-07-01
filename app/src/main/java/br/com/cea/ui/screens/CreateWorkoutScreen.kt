package br.com.cea.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cea.data.CeaDatabaseHelper
import br.com.cea.model.Exercise
import br.com.cea.model.UserProfile
import br.com.cea.model.Workout

@Composable
fun CreateWorkoutScreen(
    modifier: Modifier,
    profile: UserProfile,
    editingWorkoutId: Long?,
    database: CeaDatabaseHelper,
    selectedExercises: MutableList<Exercise>,
    onExercises: () -> Unit,
    onSave: (Workout) -> Unit
) {
    val editingWorkout = remember(editingWorkoutId) {
        editingWorkoutId?.let { database.getWorkout(it) }
    }

    var name by remember(editingWorkout) { mutableStateOf(editingWorkout?.title ?: "Push hipertrofia A") }
    var objective by remember(editingWorkout) { mutableStateOf(editingWorkout?.objective ?: profile.objective) }
    var level by remember(editingWorkout) { mutableStateOf(editingWorkout?.level ?: profile.level) }
    var duration by remember(editingWorkout) { mutableStateOf(editingWorkout?.duration ?: "60 min") }
    var muscle by remember { mutableStateOf("Peito") }

    Column(modifier) {
        CeaInput("Nome do treino", name, onValueChange = { name = it })
        Spacer(Modifier.height(14.dp))
        FieldLabel("Objetivo")
        SelectableChips(listOf("Hipertrofia", "Forca", "Cardio"), objective) { objective = it }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Nivel")
        SelectableChips(listOf("Iniciante", "Intermediario", "Avancado"), level) { level = it }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Duracao")
        SelectableChips(listOf("30 min", "45 min", "60 min"), duration) { duration = it }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Grupo muscular")
        SelectableChips(listOf("Peito", "Costas", "Pernas", "Ombros", "Braços", "Abdominais"), muscle) { muscle = it }
        Spacer(Modifier.height(16.dp))
        PrimaryAction("+ Adicionar exercicio", Modifier.fillMaxWidth(), onExercises)
        Spacer(Modifier.height(18.dp))
        SectionTitle("Exercicios selecionados")
        Spacer(Modifier.height(8.dp))
        if (selectedExercises.isEmpty()) {
            Text(
                text = "Nenhum exercício adicionado. Clique acima para adicionar.",
                color = CeaColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            selectedExercises.forEachIndexed { index, exercise ->
                ExerciseRow(
                    index = (index + 1).toString(),
                    exercise = exercise,
                    onClick = {
                        selectedExercises.removeAt(index)
                    }
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryAction("Salvar treino", Modifier.fillMaxWidth()) {
            onSave(
                Workout(
                    title = name.ifBlank { "Meu Treino" },
                    objective = objective,
                    level = level,
                    duration = duration,
                    publicWorkout = false,
                    exercises = selectedExercises.map { it.name }
                )
            )
        }
    }
}

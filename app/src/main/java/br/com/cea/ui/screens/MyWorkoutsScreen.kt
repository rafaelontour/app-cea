package br.com.cea.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cea.model.Workout

@Composable
fun MyWorkoutsScreen(
    modifier: Modifier,
    workouts: List<Workout>,
    onNew: () -> Unit,
    onStart: (Workout) -> Unit,
    onEdit: (Workout) -> Unit,
    onDuplicate: (Workout) -> Unit,
    onDelete: (Workout) -> Unit
) {
    val predefined = remember(workouts) { workouts.filter { it.publicWorkout } }
    val myWorkouts = remember(workouts) { workouts.filter { !it.publicWorkout } }
    var workoutToDelete by remember { mutableStateOf<Workout?>(null) }

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            SectionTitle("Meus Treinos")
            PrimaryAction("Novo", onClick = onNew)
        }
        Spacer(Modifier.height(10.dp))
        if (myWorkouts.isEmpty()) {
            CeaCard {
                Text(
                    text = "Nenhum treino criado ainda.",
                    color = CeaColors.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Monte um treino próprio ou importe um plano pronto para começar.",
                    color = CeaColors.Muted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                PrimaryAction("Criar primeiro treino", Modifier.fillMaxWidth(), onClick = onNew)
            }
        } else {
            myWorkouts.forEach { workout ->
                val origin = if (workout.imported && workout.origin != null) " - Importado de ${workout.origin}" else ""
                WorkoutRow(
                    title = workout.title,
                    subtitle = "${workout.objective} - ${workout.duration}$origin",
                    action = "Iniciar",
                    onStart = { onStart(workout) },
                    onEdit = { onEdit(workout) },
                    onDuplicate = { onDuplicate(workout) },
                    onDelete = { workoutToDelete = workout }
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("Treinos Predefinidos")
        Spacer(Modifier.height(10.dp))
        if (predefined.isEmpty()) {
            Text(
                text = "Nenhum treino predefinido cadastrado.",
                color = CeaColors.Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            predefined.forEach { workout ->
                WorkoutRow(
                    title = workout.title,
                    subtitle = "${workout.objective} - ${workout.level} - ${workout.duration}",
                    action = "Iniciar",
                    onStart = { onStart(workout) },
                    onEdit = { onEdit(workout) },
                    onDuplicate = { onDuplicate(workout) },
                    onDelete = { workoutToDelete = workout }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    val pendingDelete = workoutToDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { workoutToDelete = null },
            title = {
                Text("Excluir treino", color = CeaColors.Text, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Excluir \"${pendingDelete.title}\"? Esta ação não pode ser desfeita.",
                    color = CeaColors.Muted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(pendingDelete)
                        workoutToDelete = null
                    }
                ) {
                    Text("Excluir", color = CeaColors.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutToDelete = null }) {
                    Text("Cancelar", color = CeaColors.Text, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CeaColors.Card
        )
    }
}

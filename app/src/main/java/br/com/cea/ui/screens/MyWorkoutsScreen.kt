package br.com.cea.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
            Text(
                text = "Nenhum treino criado ainda. Clique em Novo para criar um!",
                color = CeaColors.Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
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
                    onDelete = { onDelete(workout) }
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
                    onDelete = { onDelete(workout) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

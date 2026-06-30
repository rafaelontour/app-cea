package br.com.cea.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            PrimaryAction("Novo", onClick = onNew)
        }
        Spacer(Modifier.height(12.dp))
        workouts.forEach { workout ->
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
}

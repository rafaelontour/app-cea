package br.com.cea.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.cea.model.Workout

@Composable
fun ExploreScreen(
    modifier: Modifier,
    workouts: List<Workout>,
    onCalendar: () -> Unit,
    onImport: (Workout) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Objetivo") }

    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CeaInput("Pesquisar treino, musculo ou objetivo", query, Modifier.weight(1f)) { query = it }
            GhostAction("Agenda", onCalendar)
        }
        Spacer(Modifier.height(12.dp))
        SelectableChips(listOf("Objetivo", "Nivel", "Musculo", "Duracao"), filter) { filter = it }
        Spacer(Modifier.height(14.dp))
        workouts
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) || it.objective.contains(query, ignoreCase = true) }
            .forEach { workout ->
                WorkoutRow(
                    title = workout.title,
                    subtitle = "${workout.objective} - ${workout.level} - ${workout.duration}",
                    action = "Importar",
                    onStart = { onImport(workout) }
                )
                Spacer(Modifier.height(10.dp))
            }
    }
}

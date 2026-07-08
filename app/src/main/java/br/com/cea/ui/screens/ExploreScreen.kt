package br.com.cea.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val filteredWorkouts = remember(workouts, query, filter) {
        workouts.filter { workout ->
            if (query.isBlank()) {
                true
            } else {
                when (filter) {
                    "Nível" -> workout.level.contains(query, ignoreCase = true)
                    else -> workout.title.contains(query, ignoreCase = true) ||
                        workout.objective.contains(query, ignoreCase = true)
                }
            }
        }
    }

    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CeaInput("Pesquisar treino ou objetivo", query, Modifier.weight(1f)) { query = it }
            GhostAction("Agenda", onCalendar)
        }
        Spacer(Modifier.height(12.dp))
        SelectableChips(listOf("Objetivo", "Nível"), filter) { filter = it }
        Spacer(Modifier.height(14.dp))
        if (filteredWorkouts.isEmpty()) {
            CeaCard {
                Text(
                    text = "Nenhum treino encontrado.",
                    color = CeaColors.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Tente outro termo ou limpe a busca para ver os treinos disponíveis.",
                    color = CeaColors.Muted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                PrimaryAction("Limpar busca", Modifier.fillMaxWidth()) {
                    query = ""
                    filter = "Objetivo"
                }
            }
        } else {
            filteredWorkouts.forEach { workout ->
                WorkoutRow(
                    title = workout.title,
                    subtitle = "${workout.objective} - ${workout.level}",
                    action = "Importar",
                    onStart = { onImport(workout) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

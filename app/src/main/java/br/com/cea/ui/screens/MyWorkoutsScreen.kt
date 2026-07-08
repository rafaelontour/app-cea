package br.com.cea.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    onDelete: (Workout) -> Unit,
    onImport: (Workout) -> Unit
) {
    val predefined = remember(workouts) { workouts.filter { it.publicWorkout } }
    val myWorkouts = remember(workouts) { workouts.filter { !it.publicWorkout } }
    var selectedTab by remember { mutableStateOf("Meus") }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Objetivo") }
    var workoutToDelete by remember { mutableStateOf<Workout?>(null) }

    val exploreWorkouts = remember(predefined, query, filter) {
        predefined.filter { workout ->
            if (query.isBlank()) {
                true
            } else {
                when (filter) {
                    "Nivel" -> workout.level.contains(query, ignoreCase = true)
                    else -> workout.title.contains(query, ignoreCase = true) ||
                        workout.objective.contains(query, ignoreCase = true)
                }
            }
        }
    }

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle("Treinos")
            PrimaryAction("Novo", onClick = onNew)
        }
        Spacer(Modifier.height(10.dp))
        SelectableChips(listOf("Meus", "Explorar", "Predefinidos"), selectedTab) { selectedTab = it }
        Spacer(Modifier.height(14.dp))

        when (selectedTab) {
            "Explorar" -> ExploreWorkoutsTab(
                workouts = exploreWorkouts,
                query = query,
                filter = filter,
                onQueryChange = { query = it },
                onFilterChange = { filter = it },
                onImport = onImport
            )
            "Predefinidos" -> PredefinedWorkoutsTab(
                workouts = predefined,
                onStart = onStart,
                onDuplicate = onDuplicate
            )
            else -> MyWorkoutsTab(
                workouts = myWorkouts,
                onNew = onNew,
                onStart = onStart,
                onEdit = onEdit,
                onDuplicate = onDuplicate,
                onDeleteRequest = { workoutToDelete = it }
            )
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
                    "Excluir \"${pendingDelete.title}\"? Esta acao nao pode ser desfeita.",
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

@Composable
private fun MyWorkoutsTab(
    workouts: List<Workout>,
    onNew: () -> Unit,
    onStart: (Workout) -> Unit,
    onEdit: (Workout) -> Unit,
    onDuplicate: (Workout) -> Unit,
    onDeleteRequest: (Workout) -> Unit
) {
    if (workouts.isEmpty()) {
        CeaCard {
            Text(
                text = "Nenhum treino criado ainda.",
                color = CeaColors.Text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Monte um treino proprio ou importe um plano pronto para comecar.",
                color = CeaColors.Muted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            PrimaryAction("Criar primeiro treino", Modifier.fillMaxWidth(), onClick = onNew)
        }
    } else {
        workouts.forEach { workout ->
            val origin = if (workout.imported && workout.origin != null) " - Importado de ${workout.origin}" else ""
            WorkoutRow(
                title = workout.title,
                subtitle = "${workout.objective}$origin",
                action = "Iniciar",
                onStart = { onStart(workout) },
                onEdit = { onEdit(workout) },
                onDuplicate = { onDuplicate(workout) },
                onDelete = { onDeleteRequest(workout) }
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ExploreWorkoutsTab(
    workouts: List<Workout>,
    query: String,
    filter: String,
    onQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onImport: (Workout) -> Unit
) {
    CeaInput("Pesquisar treino ou objetivo", query, onValueChange = onQueryChange)
    Spacer(Modifier.height(10.dp))
    SelectableChips(listOf("Objetivo", "Nivel"), filter, onFilterChange)
    Spacer(Modifier.height(14.dp))
    if (workouts.isEmpty()) {
        CeaCard {
            Text("Nenhum treino encontrado.", color = CeaColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Tente outro termo ou limpe a busca.", color = CeaColors.Muted, fontSize = 12.sp)
        }
    } else {
        workouts.forEach { workout ->
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

@Composable
private fun PredefinedWorkoutsTab(
    workouts: List<Workout>,
    onStart: (Workout) -> Unit,
    onDuplicate: (Workout) -> Unit
) {
    if (workouts.isEmpty()) {
        Text(
            text = "Nenhum treino predefinido cadastrado.",
            color = CeaColors.Muted,
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    } else {
        workouts.forEach { workout ->
            WorkoutRow(
                title = workout.title,
                subtitle = "${workout.objective} - ${workout.level}",
                action = "Iniciar",
                onStart = { onStart(workout) },
                onDuplicate = { onDuplicate(workout) }
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

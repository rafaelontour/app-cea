package br.com.cea.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import br.com.cea.model.Exercise
import br.com.cea.model.UserProfile

@Composable
fun ExercisesScreen(
    modifier: Modifier,
    profile: UserProfile,
    exercises: List<Exercise>,
    selectedExercises: MutableList<Exercise>,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") }
    var levelFilter by remember { mutableStateOf("") }
    var equipmentFilter by remember { mutableStateOf("") }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var currentPage by remember(muscle, levelFilter, equipmentFilter, query) { mutableIntStateOf(0) }

    val filtered = exercises.filter {
        (muscle.isBlank() || it.muscleGroup == muscle) &&
        (levelFilter.isBlank() || it.level == levelFilter) &&
        (query.isBlank() || it.name.contains(query, ignoreCase = true)) &&
        (equipmentFilter.isBlank() ||
            (equipmentFilter == "sem" && it.equipment == "peso-do-corpo") ||
            (equipmentFilter == "com" && it.equipment != "peso-do-corpo"))
    }

    val pageSize = 30
    val totalPages = maxOf(1, (filtered.size + pageSize - 1) / pageSize)
    val pageStart = currentPage * pageSize
    val displayList = filtered.drop(pageStart).take(pageSize)

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Catálogo", color = CeaColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (selectedExercises.isNotEmpty()) {
                SmallAction("Confirmar (${selectedExercises.size})", onClick = onBack)
            } else {
                GhostAction("Voltar", onClick = onBack)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CeaInput(
                label = "Buscar exercicio",
                value = query,
                modifier = Modifier.weight(1f),
                onValueChange = { query = it }
            )
            Button(
                onClick = { showFilterDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CeaColors.Card),
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.height(58.dp)
            ) {
                Text("Filtrar", color = CeaColors.Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (muscle.isNotBlank() || levelFilter.isNotBlank() || equipmentFilter.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filtros ativos:", color = CeaColors.Muted, fontSize = 11.sp)
                if (muscle.isNotBlank()) {
                    ActiveFilterChip(text = muscle, onClear = { muscle = "" })
                }
                if (levelFilter.isNotBlank()) {
                    ActiveFilterChip(text = levelFilter, onClear = { levelFilter = "" })
                }
                if (equipmentFilter.isNotBlank()) {
                    val label = if (equipmentFilter == "sem") "Sem Equipamento" else "Com Equipamento"
                    ActiveFilterChip(text = label, onClear = { equipmentFilter = "" })
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        if (filtered.isEmpty()) {
            CeaCard {
                Text("Nenhum exercicio encontrado", color = CeaColors.Text, fontWeight = FontWeight.Bold)
                Text("Ajuste os filtros ou o termo de busca.", color = CeaColors.Muted, fontSize = 12.sp)
            }
        } else {
            displayList.forEachIndexed { index, exercise ->
                val absoluteIndex = pageStart + index + 1
                ExerciseRow(
                    index = absoluteIndex.toString(),
                    exercise = exercise,
                    onClick = { selectedExercise = exercise }
                )
            }
            Spacer(Modifier.height(14.dp))
            if (totalPages > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentPage > 0) {
                        TextButton(onClick = { currentPage-- }) {
                            Text("< Anterior", color = CeaColors.Green, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(Modifier.width(90.dp))
                    }
                    Text(
                        text = "Página ${currentPage + 1} de $totalPages",
                        color = CeaColors.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (currentPage < totalPages - 1) {
                        TextButton(onClick = { currentPage++ }) {
                            Text("Próxima >", color = CeaColors.Green, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(Modifier.width(90.dp))
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            currentMuscle = muscle,
            currentLevel = levelFilter,
            currentEquipment = equipmentFilter,
            onApply = { m, l, eq ->
                muscle = m
                levelFilter = l
                equipmentFilter = eq
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    if (selectedExercise != null) {
        ExerciseDetailsDialog(
            exercise = selectedExercise!!,
            selectedExercises = selectedExercises,
            profile = profile,
            onDismiss = { selectedExercise = null }
        )
    }
}

@Composable
fun ActiveFilterChip(text: String, onClear: () -> Unit) {
    Surface(
        color = CeaColors.CardAlt,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(onClick = onClear)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text, color = CeaColors.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text("×", color = CeaColors.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FilterDialog(
    currentMuscle: String,
    currentLevel: String,
    currentEquipment: String,
    onApply: (muscle: String, level: String, equipment: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMuscle by remember { mutableStateOf(currentMuscle) }
    var selectedLevel by remember { mutableStateOf(currentLevel) }
    var selectedEquipment by remember { mutableStateOf(currentEquipment) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CeaColors.Card),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtrar Exercícios",
                        color = CeaColors.Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = CeaColors.Muted, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Grupo Muscular", color = CeaColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                val muscles = listOf("Todos", "Peito", "Costas", "Pernas", "Ombros", "Braços", "Abdominais", "Outros")
                muscles.chunked(3).forEach { rowMuscles ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        rowMuscles.forEach { muscle ->
                            val isSelected = (muscle == "Todos" && selectedMuscle.isBlank()) || (muscle == selectedMuscle)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CeaColors.Green else CeaColors.CardAlt)
                                    .clickable {
                                        selectedMuscle = if (muscle == "Todos") "" else muscle
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = muscle,
                                    color = if (isSelected) Color.Black else CeaColors.Text,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (rowMuscles.size < 3) {
                            repeat(3 - rowMuscles.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text("Nível de Dificuldade", color = CeaColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                val levels = listOf("Todos", "Iniciante", "Intermediario", "Avancado")
                levels.chunked(2).forEach { rowLevels ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        rowLevels.forEach { level ->
                            val isSelected = (level == "Todos" && selectedLevel.isBlank()) || (level == selectedLevel)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CeaColors.Green else CeaColors.CardAlt)
                                    .clickable {
                                        selectedLevel = if (level == "Todos") "" else level
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = level,
                                    color = if (isSelected) Color.Black else CeaColors.Text,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (rowLevels.size < 2) {
                            repeat(2 - rowLevels.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                Text("Equipamento", color = CeaColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                val equipments = listOf(
                    Pair("Todos", ""),
                    Pair("Sem Equipamento", "sem"),
                    Pair("Com Equipamento", "com")
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    equipments.forEach { (label, value) ->
                        val isSelected = selectedEquipment == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CeaColors.Green else CeaColors.CardAlt)
                                .clickable { selectedEquipment = value }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else CeaColors.Text,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            selectedMuscle = ""
                            selectedLevel = ""
                            selectedEquipment = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CeaColors.CardAlt),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Limpar", color = CeaColors.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onApply(selectedMuscle, selectedLevel, selectedEquipment)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CeaColors.Green),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Aplicar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseDetailsDialog(
    exercise: Exercise,
    selectedExercises: MutableList<Exercise>,
    profile: UserProfile,
    onDismiss: () -> Unit
) {
    val isAlreadyAdded = selectedExercises.any { it.name == exercise.name }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CeaColors.Card),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = exercise.name,
                        color = CeaColors.Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Fechar", color = CeaColors.Green, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(exercise.muscleGroup, CeaColors.Blue)
                    StatusPill(exercise.level, CeaColors.Green)
                }
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isAlreadyAdded) {
                            selectedExercises.removeAll { it.name == exercise.name }
                            onDismiss()
                        } else {
                            if (profile.level == "Iniciante" && exercise.level != "Iniciante") {
                                showConfirmDialog = true
                            } else {
                                selectedExercises.add(exercise)
                                onDismiss()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAlreadyAdded) CeaColors.Red else CeaColors.Green
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isAlreadyAdded) "Remover do Treino" else "Adicionar ao Treino",
                        color = if (isAlreadyAdded) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(16.dp))

                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        title = {
                            Text(
                                text = "Confirmar Exercício",
                                color = CeaColors.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        },
                        text = {
                            Text(
                                text = "Você selecionou o nível 'Iniciante' no seu perfil, mas este exercício é de nível '${exercise.level}'. Tem certeza que deseja adicioná-lo ao seu treino?",
                                color = CeaColors.Muted,
                                fontSize = 14.sp
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    selectedExercises.add(exercise)
                                    showConfirmDialog = false
                                    onDismiss()
                                }
                            ) {
                                Text("Sim, tenho certeza", color = CeaColors.Green, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showConfirmDialog = false }
                            ) {
                                Text("Cancelar", color = CeaColors.Red, fontWeight = FontWeight.Bold)
                            }
                        },
                        containerColor = CeaColors.Card,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                if (exercise.primaryMuscles.isNotBlank()) {
                    Text("Regiões Primárias:", color = CeaColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        exercise.primaryMuscles.split(",").map { it.trim().replaceFirstChar { c -> c.uppercase() } }.forEach { muscle ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CeaColors.CardAlt)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(muscle, color = CeaColors.Text, fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (exercise.secondaryMuscles.isNotBlank()) {
                    Text("Regiões Secundárias:", color = CeaColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        exercise.secondaryMuscles.split(",").map { it.trim().replaceFirstChar { c -> c.uppercase() } }.forEach { muscle ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CeaColors.CardAlt)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(muscle, color = CeaColors.Muted, fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text("Instruções:", color = CeaColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                val steps = exercise.instructions.split("\n").filter { it.isNotBlank() }
                steps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "${index + 1}. ",
                            color = CeaColors.Green,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = step,
                            color = CeaColors.Muted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

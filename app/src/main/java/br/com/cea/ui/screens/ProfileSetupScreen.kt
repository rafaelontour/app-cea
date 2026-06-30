package br.com.cea.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cea.model.UserProfile

@Composable
fun ProfileSetupScreen(profile: UserProfile, onSave: (UserProfile) -> Unit) {
    var name by remember { mutableStateOf(profile.name) }
    var age by remember { mutableStateOf(profile.age.toString()) }
    var weight by remember { mutableStateOf(profile.weightKg.toString()) }
    var height by remember { mutableStateOf(profile.heightCm.toString()) }
    var objective by remember { mutableStateOf(profile.objective) }
    var level by remember { mutableStateOf(profile.level) }
    var frequency by remember { mutableStateOf(profile.frequencyPerWeek.toString()) }
    var hours by remember { mutableStateOf(profile.hoursPerDay.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text("Configure seu Perfil", color = CeaColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))

        CeaInput("Nome", name) { name = it }
        Spacer(Modifier.height(12.dp))
        CeaInput("Idade", age, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) { age = it }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CeaInput("Peso (kg)", weight, Modifier.weight(1f), androidx.compose.ui.text.input.KeyboardType.Number) { weight = it }
            CeaInput("Altura (cm)", height, Modifier.weight(1f), androidx.compose.ui.text.input.KeyboardType.Number) { height = it }
        }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Objetivo")
        SelectableChips(listOf("Hipertrofia", "Forca", "Cardio"), objective) { objective = it }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Nivel de treino")
        SelectableChips(listOf("Iniciante", "Intermediario", "Avancado"), level) { level = it }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CeaInput("Frequencia semanal (dias)", frequency, Modifier.weight(1f), androidx.compose.ui.text.input.KeyboardType.Number) { frequency = it }
            CeaInput("Tempo diario (horas)", hours, Modifier.weight(1f), androidx.compose.ui.text.input.KeyboardType.Number) { hours = it }
        }
        Spacer(Modifier.height(24.dp))
        PrimaryAction("Salvar e Continuar", Modifier.fillMaxWidth()) {
            onSave(
                profile.copy(
                    name = name.ifBlank { "Usuario" },
                    age = age.toIntOrNull() ?: 20,
                    weightKg = weight.toDoubleOrNull() ?: 70.0,
                    heightCm = height.toDoubleOrNull() ?: 175.0,
                    objective = objective,
                    level = level,
                    frequencyPerWeek = frequency.toIntOrNull() ?: 3,
                    hoursPerDay = hours.toDoubleOrNull() ?: 1.0
                )
            )
        }
    }
}

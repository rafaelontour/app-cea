package br.com.cea.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import br.com.cea.model.UserProfile

@Composable
fun ProfileSetupScreen(profile: UserProfile, onSave: (UserProfile) -> Unit) {
    var name by remember { mutableStateOf(profile.name) }
    var age by remember { mutableStateOf(profile.age.toInputText()) }
    var weight by remember { mutableStateOf(profile.weightKg.toInputText()) }
    var height by remember { mutableStateOf(profile.heightCm.toInputText()) }
    var objective by remember { mutableStateOf(profile.objective.normalizedObjective()) }
    var level by remember { mutableStateOf(profile.level.normalizedLevel()) }
    var frequency by remember { mutableStateOf(profile.frequencyPerWeek.toInputText()) }
    var hours by remember { mutableStateOf(profile.hoursPerDay.toInputText()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        CeaBrandLockup(Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))

        CeaCard {
            Text(
                text = "Seu plano inicial está pronto",
                color = CeaColors.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EntrySummaryPill("${frequency.ifBlank { "3" }}x/sem", Modifier.weight(1f))
                EntrySummaryPill(objective, Modifier.weight(1f))
                EntrySummaryPill(level, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Dados pessoais")
        Spacer(Modifier.height(8.dp))
        CeaCard {
            CeaInput("Nome", name) { name = it }
            Spacer(Modifier.height(12.dp))
            CeaInput("Idade", age, keyboardType = KeyboardType.Number) { age = it }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CeaInput("Peso (kg)", weight, Modifier.weight(1f), KeyboardType.Number) { weight = it }
                CeaInput("Altura (cm)", height, Modifier.weight(1f), KeyboardType.Number) { height = it }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Preferências de treino")
        Spacer(Modifier.height(8.dp))
        CeaCard {
            FieldLabel("Objetivo")
            SelectableChips(listOf("Hipertrofia", "Força", "Cardio"), objective) { objective = it }
            Spacer(Modifier.height(14.dp))
            FieldLabel("Nível de treino")
            SelectableChips(listOf("Iniciante", "Intermediário", "Avançado"), level) { level = it }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CeaInput("Frequência semanal", frequency, Modifier.weight(1f), KeyboardType.Number) { frequency = it }
                CeaInput("Horas por dia", hours, Modifier.weight(1f), KeyboardType.Number) { hours = it }
            }
        }

        Spacer(Modifier.height(18.dp))
        PrimaryAction("Entrar no CEA", Modifier.fillMaxWidth()) {
            onSave(
                profile.copy(
                    name = name.ifBlank { "Usuário" },
                    age = age.toIntOrNull() ?: 0,
                    weightKg = weight.toDoubleOrNull() ?: 0.0,
                    heightCm = height.toDoubleOrNull() ?: 0.0,
                    objective = objective,
                    level = level,
                    frequencyPerWeek = frequency.toIntOrNull() ?: 3,
                    hoursPerDay = hours.toDoubleOrNull() ?: 1.0
                )
            )
        }
    }
}

private fun Int.toInputText(): String {
    return if (this > 0) toString() else ""
}

private fun Double.toInputText(): String {
    return if (this > 0.0) toString() else ""
}

@Composable
private fun EntrySummaryPill(text: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Surface(
        modifier = modifier.heightIn(min = 34.dp),
        color = CeaColors.CardAlt,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
            Text(
                text = text,
                color = CeaColors.Green,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

private fun String.normalizedObjective(): String {
    return when (this) {
        "Forca" -> "Força"
        else -> ifBlank { "Hipertrofia" }
    }
}

private fun String.normalizedLevel(): String {
    return when (this) {
        "Intermediario" -> "Intermediário"
        "Avancado" -> "Avançado"
        else -> ifBlank { "Iniciante" }
    }
}

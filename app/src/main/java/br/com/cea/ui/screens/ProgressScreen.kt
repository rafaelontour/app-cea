package br.com.cea.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cea.R
import br.com.cea.data.CeaDatabaseHelper
import br.com.cea.model.UserProfile
import br.com.cea.model.WeightLog
import br.com.cea.service.BmiService

@Composable
fun ProgressScreen(
    modifier: Modifier,
    profile: UserProfile,
    bmiService: BmiService,
    database: CeaDatabaseHelper,
    onWeightLogged: () -> Unit
) {
    var weightInput by remember { mutableStateOf("") }
    var refreshHistory by remember { mutableIntStateOf(0) }
    val history = remember(refreshHistory) { database.getWeightHistory() }
    val currentWeight = remember(history, profile) {
        history.lastOrNull()?.weightKg ?: profile.weightKg
    }
    val bmi = remember(currentWeight, profile.heightCm) {
        bmiService.calculate(currentWeight, profile.heightCm)
    }

    Column(modifier) {
        val completedCount = remember(profile) { database.getCompletedWorkoutsCount() }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard((18 + completedCount).toString(), "Concluídos", Modifier.weight(1f))
            MetricCard("24", "Dias ativos", Modifier.weight(1f))
            MetricCard("7", "Sequência", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))

        CeaCard {
            SectionTitle("Novo registro de peso")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CeaInput(
                    label = "Peso (kg)",
                    value = weightInput,
                    modifier = Modifier.weight(1f),
                    onValueChange = { weightInput = it }
                )
                Button(
                    onClick = {
                        val parsed = weightInput.toDoubleOrNull()
                        if (parsed != null && parsed > 0) {
                            database.logWeight(parsed, profile.heightCm)
                            weightInput = ""
                            refreshHistory++
                            onWeightLogged()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CeaColors.Green),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.height(58.dp)
                ) {
                    Text("Registrar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (history.isNotEmpty()) {
            CeaCard {
                SectionTitle("Evolução do peso")
                Spacer(Modifier.height(12.dp))
                WeightProgressionChart(history)
            }
            Spacer(Modifier.height(14.dp))
        }

        CeaCard {
            SectionTitle("Frequência semanal")
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                listOf(30, 46, 40, 54, 72, 100, 58).zip(listOf("S", "T", "Q", "Q", "S", "S", "D")).forEach { (value, day) ->
                    DayBar(day, value)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        CeaCard {
            SectionTitle("Cargas principais")
            Spacer(Modifier.height(10.dp))
            ProgressLine("Supino", 60, 95)
            ProgressLine("Agachamento", 95, 100)
            ProgressLine("Remada", 54, 80)
        }
        Spacer(Modifier.height(14.dp))
        CeaCard {
            SectionTitle("Evolução corporal")
            Text("IMC: ${"%.1f".format(bmi)} - ${bmiService.classify(bmi)}", color = CeaColors.Muted, fontSize = 12.sp)
            Text(
                text = LocalContext.current.getString(R.string.bmi_warning),
                color = CeaColors.Muted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun WeightProgressionChart(history: List<WeightLog>) {
    if (history.isEmpty()) return
    val maxWeight = history.maxOf { it.weightKg }
    val minWeight = history.minOf { it.weightKg }
    val weightRange = (maxWeight - minWeight).coerceAtLeast(1.0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        history.takeLast(7).forEach { log ->
            val fraction = if (weightRange > 0) {
                0.2f + 0.8f * ((log.weightKg - minWeight) / weightRange).toFloat()
            } else {
                0.6f
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "${log.weightKg}kg",
                    color = CeaColors.Text,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((60 * fraction).dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CeaColors.Green)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = log.date,
                    color = CeaColors.Muted,
                    fontSize = 9.sp
                )
            }
        }
    }
}

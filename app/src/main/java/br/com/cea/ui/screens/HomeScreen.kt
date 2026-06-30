package br.com.cea.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    modifier: Modifier,
    onCreateWorkout: () -> Unit,
    onMyWorkouts: () -> Unit,
    onExplore: () -> Unit,
    onExercises: () -> Unit
) {
    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShortcutCard("🏋️", "Criar treino", Modifier.weight(1f), onCreateWorkout)
            ShortcutCard("📋", "Meus planos", Modifier.weight(1f), onMyWorkouts)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShortcutCard("🌐", "Explorar", Modifier.weight(1f), onExplore)
            ShortcutCard("🔍", "Exercicios", Modifier.weight(1f), onExercises)
        }
        Spacer(Modifier.height(20.dp))
        CeaCard {
            SectionTitle("Dica do dia")
            Spacer(Modifier.height(6.dp))
            Text(
                "Mantenha a constancia. A tecnica correta nos exercicios previne lesoes e maximiza seus resultados a longo prazo.",
                color = CeaColors.Muted,
                fontSize = 11.sp
            )
        }
    }
}

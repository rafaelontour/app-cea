package br.com.cea.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object CeaColors {
    val Black = Color(0xFF07090C)
    val Surface = Color(0xFF12161D)
    val Card = Color(0xFF1B2028)
    val CardAlt = Color(0xFF232A33)
    val Text = Color(0xFFF5F7FA)
    val Muted = Color(0xFF8E98A8)
    val Green = Color(0xFF35FF78)
    val Blue = Color(0xFF2D8CFF)
    val Red = Color(0xFFFF4757)
}

enum class Screen(val tab: String, val label: String) {
    ProfileSetup("none", "Info"),
    Home("home", "Início"),
    CreateWorkout("workouts", "Criar"),
    MyWorkouts("workouts", "Treinos"),
    Explore("workouts", "Explorar"),
    Progress("progress", "Progresso"),
    Exercises("exercises", "Exercícios"),
    Calendar("calendar", "Calendário"),
    Profile("profile", "Perfil"),
    ActiveWorkout("workouts", "Ativo")
}

@Composable
fun CeaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = CeaColors.Black,
            surface = CeaColors.Surface,
            primary = CeaColors.Green,
            secondary = CeaColors.Blue,
            onPrimary = Color.Black,
            onSurface = CeaColors.Text,
            onBackground = CeaColors.Text
        ),
        content = content
    )
}

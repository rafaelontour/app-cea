package br.com.cea.model

data class UserProfile(
    val id: Long = 0,
    val name: String = "Cainan",
    val age: Int = 18,
    val weightKg: Double = 70.0,
    val heightCm: Double = 175.0,
    val activityLevel: String = "Treina regularmente",
    val level: String = "Intermediário",
    val objective: String = "Hipertrofia",
    val frequencyPerWeek: Int = 7,
    val hoursPerDay: Double = 1.0,
    val publicProfile: Boolean = true
)

data class Exercise(
    val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val level: String,
    val instructions: String,
    val imageUri: String = "",
    val primaryMuscles: String = "",
    val secondaryMuscles: String = "",
    val equipment: String = ""
)

data class Workout(
    val id: Long = 0,
    val title: String,
    val objective: String,
    val level: String,
    val duration: String,
    val publicWorkout: Boolean = false,
    val imported: Boolean = false,
    val origin: String? = null,
    val exercises: List<String> = emptyList()
)

data class CalendarDay(
    val day: Int,
    val state: DayState = DayState.Empty
)

enum class DayState {
    Empty,
    Completed,
    Scheduled,
    Missed
}

data class WeightLog(
    val id: Long = 0,
    val weightKg: Double,
    val date: String
)

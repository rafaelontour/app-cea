package br.com.cea.model

data class UserProfile(
    val id: Long = 0,
    val name: String = "",
    val age: Int = 0,
    val weightKg: Double = 0.0,
    val heightCm: Double = 0.0,
    val activityLevel: String = "",
    val level: String = "",
    val objective: String = "",
    val frequencyPerWeek: Int = 0,
    val hoursPerDay: Double = 0.0,
    val publicProfile: Boolean = false,
    val dailyWaterGoalMl: Int = 2500
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
    val exercises: List<String> = emptyList(),
    val exerciseSpecs: List<WorkoutExerciseSpec> = emptyList()
)

data class WorkoutExerciseSpec(
    val name: String,
    val sets: Int = 3,
    val reps: String = "10",
    val durationSeconds: Int = 0,
    val restSeconds: Int = 60
)

data class ScheduledWorkout(
    val id: Long,
    val workoutId: Long,
    val workoutTitle: String,
    val workoutObjective: String,
    val workoutDuration: String,
    val scheduledAt: Long
)

data class WorkoutHistoryEntry(
    val title: String,
    val completedAt: Long,
    val durationSeconds: Int
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

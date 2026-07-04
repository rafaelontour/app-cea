package br.com.cea.ui

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cea.data.CeaDatabaseHelper
import br.com.cea.model.Exercise
import br.com.cea.model.Workout
import br.com.cea.service.AnalyticsTracker
import br.com.cea.service.BmiService
import br.com.cea.service.HydrationReminderReceiver
import br.com.cea.service.WorkoutRecommendationService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = CeaColors.Black.toArgb()
        window.navigationBarColor = CeaColors.Black.toArgb()
        setContent {
            CeaTheme {
                CeaApp(activity = this)
            }
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 44)
        }
    }

    override fun onPause() {
        super.onPause()
        if (getPreferences(MODE_PRIVATE).getBoolean("profile_done", false)) {
            scheduleHydrationReminder()
        }
    }

    private fun scheduleHydrationReminder() {
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            29,
            Intent(this, HydrationReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60L * 60L * 1000L, pendingIntent)
    }
}

@Composable
private fun CeaApp(activity: MainActivity) {
    val context = LocalContext.current
    val database = remember { CeaDatabaseHelper(context) }
    val recommendationService = remember { WorkoutRecommendationService() }
    val bmiService = remember { BmiService() }
    val analytics = remember { AnalyticsTracker() }
    val preferences = remember { activity.getPreferences(Context.MODE_PRIVATE) }

    var profile by remember { mutableStateOf(database.getProfile()) }
    var screen by remember {
        mutableStateOf(if (preferences.getBoolean("profile_done", false)) Screen.Home else Screen.ProfileSetup)
    }
    var refresh by remember { mutableIntStateOf(0) }
    val selectedExercises = remember { mutableStateListOf<Exercise>() }
    var editingWorkoutId by remember { mutableStateOf<Long?>(null) }
    var activeWorkout by remember { mutableStateOf<Workout?>(null) }
    var exerciseMuscleFilter by remember { mutableStateOf("") }

    LaunchedEffect(screen, refresh) {
        profile = database.getProfile()
    }

    fun promoteProfileLevelIfNeeded(newLevel: String) {
        val normalizedLevel = newLevel.normalizedTrainingLevel()
        if (isTrainingLevelAbove(normalizedLevel, profile.level)) {
            val updatedProfile = profile.copy(level = normalizedLevel)
            database.saveProfile(updatedProfile)
            profile = updatedProfile
        }
    }

    if (screen == Screen.ProfileSetup) {
        ProfileSetupScreen(
            profile = profile,
            onSave = { updated ->
                database.saveProfile(updated)
                if (updated.weightKg > 0.0 && updated.heightCm > 0.0) {
                    database.logWeight(updated.weightKg, updated.heightCm)
                }
                preferences.edit().putBoolean("profile_done", true).apply()
                analytics.track(context, "profile_created")
                profile = updated
                screen = Screen.Home
            }
        )
        return
    }

    AppShell(
        screen = screen,
        title = when (screen) {
            Screen.Home -> "Olá, ${profile.name.firstName()}"
            Screen.CreateWorkout -> "Criar novo treino"
            Screen.MyWorkouts -> "Meus treinos"
            Screen.Explore -> "Treinos"
            Screen.Progress -> "Progresso"
            Screen.Exercises -> "Exercícios"
            Screen.Calendar -> "Calendário"
            Screen.Profile -> "Perfil"
            Screen.ActiveWorkout -> "Treino Ativo"
            Screen.ProfileSetup -> ""
        },
        subtitle = when (screen) {
            Screen.Home -> "Pronto para evoluir hoje?"
            Screen.CreateWorkout -> "Monte um plano personalizado"
            Screen.Progress -> "Sua evolução em números"
            Screen.Calendar -> "Sua rotina de treinos"
            Screen.ActiveWorkout -> "Mantenha o foco!"
            else -> ""
        },
        onNavigate = { screen = it }
    ) { modifier ->
        when (screen) {
            Screen.Home -> HomeScreen(
                modifier = modifier,
                onCreateWorkout = { screen = Screen.CreateWorkout },
                onMyWorkouts = { screen = Screen.MyWorkouts },
                onExplore = { screen = Screen.Explore },
                onExercises = {
                    exerciseMuscleFilter = ""
                    screen = Screen.Exercises
                }
            )
            Screen.CreateWorkout -> CreateWorkoutScreen(
                modifier = modifier,
                profile = profile,
                editingWorkoutId = editingWorkoutId,
                database = database,
                selectedExercises = selectedExercises,
                onExercises = { muscle ->
                    exerciseMuscleFilter = muscle
                    screen = Screen.Exercises
                },
                onSave = { workout ->
                    promoteProfileLevelIfNeeded(workout.level)
                    val workoutToSave = workout.copy(level = workout.level.normalizedTrainingLevel())
                    if (editingWorkoutId != null) {
                        database.updateWorkout(workoutToSave.copy(id = editingWorkoutId!!))
                        analytics.track(context, "workout_updated")
                    } else {
                        database.saveWorkout(workoutToSave)
                        analytics.track(context, "workout_generated")
                    }
                    editingWorkoutId = null
                    selectedExercises.clear()
                    refresh++
                    screen = Screen.MyWorkouts
                }
            )
            Screen.MyWorkouts -> MyWorkoutsScreen(
                modifier = modifier,
                workouts = database.listWorkouts(publicOnly = false),
                onNew = {
                    editingWorkoutId = null
                    selectedExercises.clear()
                    screen = Screen.CreateWorkout
                },
                onStart = { workout ->
                    activeWorkout = workout
                    screen = Screen.ActiveWorkout
                },
                onEdit = { workout ->
                    editingWorkoutId = workout.id
                    selectedExercises.clear()
                    val catalog = database.listExercises()
                    workout.exercises.forEach { name ->
                        catalog.find { it.name == name }?.let { selectedExercises.add(it) }
                    }
                    screen = Screen.CreateWorkout
                },
                onDuplicate = { workout ->
                    database.duplicateWorkout(workout.id)
                    refresh++
                },
                onDelete = { workout ->
                    database.deleteWorkout(workout.id)
                    refresh++
                }
            )
            Screen.Explore -> ExploreScreen(
                modifier = modifier,
                workouts = database.listWorkouts(publicOnly = true),
                onCalendar = { screen = Screen.Calendar },
                onImport = { workout ->
                    database.importWorkout(workout, profile.name.ifBlank { "Usuário" })
                    analytics.track(context, "workout_imported")
                    refresh++
                    screen = Screen.MyWorkouts
                }
            )
            Screen.Progress -> ProgressScreen(
                modifier = modifier,
                profile = profile,
                bmiService = bmiService,
                database = database,
                onWeightLogged = {
                    refresh++
                }
            )
            Screen.Exercises -> ExercisesScreen(
                modifier = modifier,
                profile = profile,
                exercises = database.listExercises(),
                selectedExercises = selectedExercises,
                initialMuscleFilter = exerciseMuscleFilter,
                onLevelIncrease = { newLevel ->
                    promoteProfileLevelIfNeeded(newLevel)
                    refresh++
                },
                onBack = {
                    if (editingWorkoutId != null || selectedExercises.isNotEmpty() || exerciseMuscleFilter.isNotBlank()) {
                        screen = Screen.CreateWorkout
                    } else {
                        screen = Screen.Home
                    }
                }
            )
            Screen.ActiveWorkout -> {
                val workout = activeWorkout
                if (workout != null) {
                    ActiveWorkoutScreen(
                        modifier = modifier,
                        workout = workout,
                        database = database,
                        onFinished = {
                            screen = Screen.MyWorkouts
                            activeWorkout = null
                            refresh++
                        }
                    )
                } else {
                    screen = Screen.MyWorkouts
                }
            }
            Screen.Calendar -> {
                val workouts = database.listWorkouts(publicOnly = false)
                val firstWorkout = workouts.firstOrNull()
                CalendarScreen(
                    modifier = modifier,
                    database = database,
                    onStart = {
                        if (firstWorkout != null) {
                            activeWorkout = firstWorkout
                            screen = Screen.ActiveWorkout
                        } else {
                            screen = Screen.MyWorkouts
                        }
                    }
                )
            }
            Screen.Profile -> ProfileScreen(
                modifier = modifier,
                profile = profile,
                waterMl = database.getTodayWaterMl(),
                database = database,
                onEdit = { screen = Screen.ProfileSetup },
                onWater = {
                    activity.requestNotificationPermission()
                    database.logWater(250)
                    analytics.track(context, "water_logged")
                    refresh++
                },
                onProgress = { screen = Screen.Progress }
            )
            Screen.ProfileSetup -> Unit
        }
    }
}

@Composable
private fun AppShell(
    screen: Screen,
    title: String,
    subtitle: String,
    onNavigate: (Screen) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        containerColor = CeaColors.Black,
        bottomBar = {
            Surface(color = CeaColors.Surface, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems().forEach { item ->
                        BottomNavPill(
                            item = item,
                            selected = screen.tab == item.tab,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(item) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Header(title = title, subtitle = subtitle)
            content(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp)
            )
        }
    }
}

private fun navItems() = listOf(Screen.Home, Screen.MyWorkouts, Screen.Progress, Screen.Exercises, Screen.Profile)

private fun Screen.icon(): ImageVector {
    return when (this) {
        Screen.Home -> Icons.Filled.Home
        Screen.MyWorkouts -> Icons.Filled.ViewList
        Screen.Progress -> Icons.Filled.ShowChart
        Screen.Exercises -> Icons.Filled.FitnessCenter
        Screen.Profile -> Icons.Filled.Person
        else -> Icons.Filled.Home
    }
}

@Composable
private fun BottomNavPill(
    item: Screen,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val foreground = if (selected) CeaColors.Green else CeaColors.Muted
    Column(
        modifier = modifier
            .height(66.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) CeaColors.Card else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 3.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (selected) CeaColors.Green else Color.Transparent)
        )
        Spacer(Modifier.height(7.dp))
        Icon(
            imageVector = item.icon(),
            contentDescription = item.label,
            tint = foreground,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = item.label.uppercase(),
            color = foreground,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Header(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = CeaColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = CeaColors.Muted, fontSize = 12.sp)
            }
        }
        Avatar()
    }
}

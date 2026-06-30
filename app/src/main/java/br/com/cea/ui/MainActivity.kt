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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import br.com.cea.R
import java.util.Calendar
import java.util.Locale
import br.com.cea.data.CeaDatabaseHelper
import br.com.cea.model.DayState
import br.com.cea.model.Exercise
import br.com.cea.model.UserProfile
import br.com.cea.model.Workout
import br.com.cea.model.WeightLog
import br.com.cea.service.AnalyticsTracker
import br.com.cea.service.BmiService
import br.com.cea.service.HydrationReminderReceiver
import br.com.cea.service.WorkoutRecommendationService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = CeaColors.Black.toArgb()
        window.navigationBarColor = CeaColors.Black.toArgb()
        requestNotificationPermission()

        setContent {
            CeaTheme {
                CeaApp(activity = this)
            }
        }
    }

    private fun requestNotificationPermission() {
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

private enum class Screen(val tab: String, val label: String) {
    ProfileSetup("none", "Info"),
    Home("home", "Inicio"),
    CreateWorkout("workouts", "Criar"),
    MyWorkouts("workouts", "Treinos"),
    Explore("workouts", "Explorar"),
    Progress("progress", "Prog"),
    Exercises("exercises", "Exer"),
    Calendar("workouts", "Calendario"),
    Profile("profile", "Perfil"),
    ActiveWorkout("workouts", "Ativo")
}

private object CeaColors {
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

@Composable
private fun CeaTheme(content: @Composable () -> Unit) {
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

    LaunchedEffect(screen, refresh) {
        profile = database.getProfile()
    }

    if (screen == Screen.ProfileSetup) {
        ProfileSetupScreen(
            profile = profile,
            onSave = { updated ->
                database.saveProfile(updated)
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
            Screen.Home -> "Ola, ${profile.name.firstName()}"
            Screen.CreateWorkout -> "Criar novo treino"
            Screen.MyWorkouts -> "Meus treinos"
            Screen.Explore -> "Treinos"
            Screen.Progress -> "Progresso"
            Screen.Exercises -> "Exercicios"
            Screen.Calendar -> "Calendario"
            Screen.Profile -> "Perfil"
            Screen.ActiveWorkout -> "Treino Ativo"
            Screen.ProfileSetup -> ""
        },
        subtitle = when (screen) {
            Screen.Home -> "Pronto para evoluir hoje?"
            Screen.CreateWorkout -> "Monte um plano personalizado"
            Screen.Progress -> "Sua evolucao em numeros"
            Screen.Calendar -> "Junho 2026"
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
                onExercises = { screen = Screen.Exercises }
            )
            Screen.CreateWorkout -> CreateWorkoutScreen(
                modifier = modifier,
                profile = profile,
                editingWorkoutId = editingWorkoutId,
                database = database,
                selectedExercises = selectedExercises,
                onExercises = { screen = Screen.Exercises },
                onSave = { workout ->
                    if (editingWorkoutId != null) {
                        database.updateWorkout(workout.copy(id = editingWorkoutId!!))
                        analytics.track(context, "workout_updated")
                    } else {
                        database.saveWorkout(workout)
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
                    database.importWorkout(workout, "Cainan")
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
            Screen.Exercises -> ExercisesScreen(modifier, database.listExercises(), selectedExercises)
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
            Screen.Calendar -> CalendarScreen(modifier, onStart = { screen = Screen.Exercises })
            Screen.Profile -> ProfileScreen(
                modifier = modifier,
                profile = profile,
                waterMl = database.getTodayWaterMl(),
                onEdit = { screen = Screen.ProfileSetup },
                onWater = {
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

@Composable
private fun Avatar() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(CeaColors.CardAlt),
        contentAlignment = Alignment.Center
    ) {
        Text("CB", color = CeaColors.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileSetupScreen(profile: UserProfile, onSave: (UserProfile) -> Unit) {
    var name by remember { mutableStateOf(profile.name) }
    var age by remember { mutableStateOf(profile.age.toString()) }
    var weight by remember { mutableStateOf(profile.weightKg.toInt().toString()) }
    var height by remember { mutableStateOf(profile.heightCm.toInt().toString()) }
    var level by remember { mutableStateOf(profile.level) }
    var objective by remember { mutableStateOf(profile.objective) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CeaColors.Black)
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))
        Text("Informacoes Pessoais", color = CeaColors.Text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        CeaCard {
            CeaInput("Nome", name, onValueChange = { name = it })
            Spacer(Modifier.height(14.dp))
            CeaInput("Idade", age, keyboardType = KeyboardType.Number, onValueChange = { age = it })
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CeaInput("Peso", weight, Modifier.weight(1f), KeyboardType.Number) { weight = it }
                CeaInput("Altura (cm)", height, Modifier.weight(1f), KeyboardType.Number) { height = it }
            }
            Spacer(Modifier.height(16.dp))
            FieldLabel("Nivel de treino")
            SelectableChips(
                options = listOf("Iniciante", "Intermediario", "Avancado"),
                selected = level,
                onSelected = { level = it }
            )
            Spacer(Modifier.height(16.dp))
            FieldLabel("Objetivo")
            SelectableChips(
                options = listOf("Hipertrofia", "Forca", "Cardio"),
                selected = objective,
                onSelected = { objective = it }
            )
            Spacer(Modifier.height(22.dp))
            PrimaryAction("Confirmar", Modifier.fillMaxWidth()) {
                onSave(
                    profile.copy(
                        name = name.ifBlank { "Cainan" },
                        age = age.toIntOrNull() ?: 18,
                        weightKg = weight.toDoubleOrNull() ?: 70.0,
                        heightCm = height.toDoubleOrNull() ?: 175.0,
                        level = level,
                        objective = objective
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    onCreateWorkout: () -> Unit,
    onMyWorkouts: () -> Unit,
    onExplore: () -> Unit,
    onExercises: () -> Unit
) {
    Column(modifier) {
        CeaCard {
            Text("Continuar treino", color = CeaColors.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Upper body forca", color = CeaColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("4 de 7 exercicios concluidos", color = CeaColors.Muted, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { 0.58f },
                modifier = Modifier.fillMaxWidth(),
                color = CeaColors.Green,
                trackColor = CeaColors.CardAlt
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                SmallAction("Retomar", onExercises)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("18", "Treinos", Modifier.weight(1f))
            MetricCard("7 dias", "Sequencia", Modifier.weight(1f))
            MetricCard("12h", "Tempo", Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        SectionTitle("Atalhos")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShortcutCard("+", "Criar novo\ntreino", Modifier.weight(1f), onCreateWorkout)
            ShortcutCard("MT", "Meus\ntreinos", Modifier.weight(1f), onMyWorkouts)
            ShortcutCard("PF", "Treinos da\nplataforma", Modifier.weight(1f), onExplore)
        }
        Spacer(Modifier.height(18.dp))
        SectionTitle("Recomendado")
        Spacer(Modifier.height(8.dp))
        WorkoutRow("Full body metabolico", "Intermediario - 45 min - 520 kcal", "Iniciar", onExercises)
    }
}

@Composable
private fun CreateWorkoutScreen(
    modifier: Modifier,
    profile: UserProfile,
    editingWorkoutId: Long?,
    database: CeaDatabaseHelper,
    selectedExercises: MutableList<Exercise>,
    onExercises: () -> Unit,
    onSave: (Workout) -> Unit
) {
    val editingWorkout = remember(editingWorkoutId) {
        editingWorkoutId?.let { database.getWorkout(it) }
    }

    var name by remember(editingWorkout) { mutableStateOf(editingWorkout?.title ?: "Push hipertrofia A") }
    var objective by remember(editingWorkout) { mutableStateOf(editingWorkout?.objective ?: profile.objective) }
    var level by remember(editingWorkout) { mutableStateOf(editingWorkout?.level ?: profile.level) }
    var duration by remember(editingWorkout) { mutableStateOf(editingWorkout?.duration ?: "60 min") }
    var muscle by remember { mutableStateOf("Peito") }

    Column(modifier) {
        CeaInput("Nome do treino", name, onValueChange = { name = it })
        Spacer(Modifier.height(14.dp))
        FieldLabel("Objetivo")
        SelectableChips(listOf("Hipertrofia", "Forca", "Cardio"), objective) { objective = it }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Nivel")
        SelectableChips(listOf("Iniciante", "Intermediario", "Avancado"), level) { level = it }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Duracao")
        SelectableChips(listOf("30 min", "45 min", "60 min"), duration) { duration = it }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Grupo muscular")
        SelectableChips(listOf("Peito", "Costas", "Pernas", "Ombros", "Braços", "Abdominais"), muscle) { muscle = it }
        Spacer(Modifier.height(16.dp))
        PrimaryAction("+ Adicionar exercicio", Modifier.fillMaxWidth(), onExercises)
        Spacer(Modifier.height(18.dp))
        SectionTitle("Exercicios selecionados")
        Spacer(Modifier.height(8.dp))
        if (selectedExercises.isEmpty()) {
            Text(
                text = "Nenhum exercício adicionado. Clique acima para adicionar.",
                color = CeaColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            selectedExercises.forEachIndexed { index, exercise ->
                ExerciseRow(
                    index = (index + 1).toString(),
                    exercise = exercise,
                    onClick = {
                        selectedExercises.removeAt(index)
                    }
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryAction("Salvar treino", Modifier.fillMaxWidth()) {
            onSave(
                Workout(
                    title = name.ifBlank { "Meu Treino" },
                    objective = objective,
                    level = level,
                    duration = duration,
                    publicWorkout = true,
                    exercises = selectedExercises.map { it.name }
                )
            )
        }
    }
}

@Composable
private fun MyWorkoutsScreen(
    modifier: Modifier,
    workouts: List<Workout>,
    onNew: () -> Unit,
    onStart: (Workout) -> Unit,
    onEdit: (Workout) -> Unit,
    onDuplicate: (Workout) -> Unit,
    onDelete: (Workout) -> Unit
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            PrimaryAction("Novo", onClick = onNew)
        }
        Spacer(Modifier.height(12.dp))
        workouts.forEach { workout ->
            val origin = if (workout.imported && workout.origin != null) " - Importado de ${workout.origin}" else ""
            WorkoutRow(
                title = workout.title,
                subtitle = "${workout.objective} - ${workout.duration}$origin",
                action = "Iniciar",
                onStart = { onStart(workout) },
                onEdit = { onEdit(workout) },
                onDuplicate = { onDuplicate(workout) },
                onDelete = { onDelete(workout) }
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ExploreScreen(
    modifier: Modifier,
    workouts: List<Workout>,
    onCalendar: () -> Unit,
    onImport: (Workout) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Objetivo") }

    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CeaInput("Pesquisar treino, musculo ou objetivo", query, Modifier.weight(1f)) { query = it }
            GhostAction("Agenda", onCalendar)
        }
        Spacer(Modifier.height(12.dp))
        SelectableChips(listOf("Objetivo", "Nivel", "Musculo", "Duracao"), filter) { filter = it }
        Spacer(Modifier.height(14.dp))
        workouts
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) || it.objective.contains(query, ignoreCase = true) }
            .forEach { workout ->
                WorkoutRow(
                    title = workout.title,
                    subtitle = "${workout.objective} - ${workout.level} - ${workout.duration}",
                    action = "Importar",
                    onStart = { onImport(workout) }
                )
                Spacer(Modifier.height(10.dp))
            }
    }
}

@Composable
private fun WeightProgressionChart(history: List<WeightLog>) {
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

@Composable
private fun ProgressScreen(
    modifier: Modifier,
    profile: UserProfile,
    bmiService: BmiService,
    database: CeaDatabaseHelper,
    onWeightLogged: () -> Unit
) {
    var weightInput by remember { mutableStateOf("") }
    var refreshHistory by remember { mutableIntStateOf(0) }
    val history = remember(refreshHistory) { database.getWeightHistory() }
    val bmi = bmiService.calculate(profile.weightKg, profile.heightCm)

    Column(modifier) {
        val completedCount = remember(profile) { database.getCompletedWorkoutsCount() }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard((18 + completedCount).toString(), "Concluidos", Modifier.weight(1f))
            MetricCard("24", "Dias ativos", Modifier.weight(1f))
            MetricCard("7", "Sequencia", Modifier.weight(1f))
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
                SectionTitle("Evolucao do peso")
                Spacer(Modifier.height(12.dp))
                WeightProgressionChart(history)
            }
            Spacer(Modifier.height(14.dp))
        }

        CeaCard {
            SectionTitle("Frequencia semanal")
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
            SectionTitle("Evolucao corporal")
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
private fun ExercisesScreen(
    modifier: Modifier,
    exercises: List<Exercise>,
    selectedExercises: MutableList<Exercise>
) {
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") } // Empty = All muscles
    var levelFilter by remember { mutableStateOf("") } // Empty = All levels
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var currentPage by remember(muscle, levelFilter, query) { mutableIntStateOf(0) }

    val filtered = exercises.filter {
        (muscle.isBlank() || it.muscleGroup == muscle) &&
        (levelFilter.isBlank() || it.level == levelFilter) &&
        (query.isBlank() || it.name.contains(query, ignoreCase = true))
    }
    
    val pageSize = 30
    val totalPages = maxOf(1, (filtered.size + pageSize - 1) / pageSize)
    val pageStart = currentPage * pageSize
    val displayList = filtered.drop(pageStart).take(pageSize)

    Column(modifier) {
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
        
        // Active filters row
        if (muscle.isNotBlank() || levelFilter.isNotBlank()) {
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

    if (selectedExercise != null) {
        ExerciseDetailsDialog(
            exercise = selectedExercise!!,
            selectedExercises = selectedExercises,
            onDismiss = { selectedExercise = null }
        )
    }

    if (showFilterDialog) {
        FilterDialog(
            currentMuscle = muscle,
            currentLevel = levelFilter,
            onApply = { m, l ->
                muscle = m
                levelFilter = l
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
private fun ActiveFilterChip(text: String, onClear: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CeaColors.Card)
            .clickable(onClick = onClear)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = CeaColors.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text("×", color = CeaColors.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FilterDialog(
    currentMuscle: String,
    currentLevel: String,
    onApply: (muscle: String, level: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMuscle by remember { mutableStateOf(currentMuscle) }
    var selectedLevel by remember { mutableStateOf(currentLevel) }

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
                
                // Muscle Group Filter
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
                
                // Level Filter
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
                
                Spacer(Modifier.height(24.dp))
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            selectedMuscle = ""
                            selectedLevel = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CeaColors.CardAlt),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Limpar", color = CeaColors.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    
                    Button(
                        onClick = {
                            onApply(selectedMuscle, selectedLevel)
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
private fun CalendarScreen(modifier: Modifier, onStart: () -> Unit) {
    var currentMonthOffset by remember { mutableIntStateOf(0) }

    val calendar = remember(currentMonthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JUNE)
            add(Calendar.MONTH, currentMonthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val monthName = remember(calendar) {
        calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR"))
            ?.replaceFirstChar { it.uppercase() } ?: ""
    }
    val year = remember(calendar) { calendar.get(Calendar.YEAR) }
    val totalDays = remember(calendar) { calendar.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val firstDayOfWeek = remember(calendar) { calendar.get(Calendar.DAY_OF_WEEK) }

    val startOffset = remember(firstDayOfWeek) {
        when (firstDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    val calendarCells = remember(startOffset, totalDays) {
        buildList {
            repeat(startOffset) { add(null) }
            for (day in 1..totalDays) { add(day) }
        }
    }

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "<",
                color = CeaColors.Green,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable { currentMonthOffset-- }
                    .padding(8.dp)
            )
            Text(
                text = "$monthName $year",
                color = CeaColors.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = ">",
                color = CeaColors.Green,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable { currentMonthOffset++ }
                    .padding(8.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("S", "T", "Q", "Q", "S", "S", "D").forEach {
                Text(
                    text = it,
                    color = CeaColors.Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        calendarCells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    if (day != null) {
                        val state = when (day) {
                            4, 10, 17, 20 -> DayState.Completed
                            8 -> DayState.Missed
                            12, 24 -> DayState.Scheduled
                            else -> DayState.Empty
                        }
                        CalendarCell(day, state)
                    } else {
                        Spacer(Modifier.size(36.dp))
                    }
                }
                if (week.size < 7) {
                    repeat(7 - week.size) {
                        Spacer(Modifier.size(36.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("Concluido", CeaColors.Green)
            StatusPill("Agendado", CeaColors.Blue)
            StatusPill("Perdido", CeaColors.Red)
        }
        Spacer(Modifier.height(16.dp))
        WorkoutRow("Hoje", "Lower forca - 18:30 - Pernas - 50 min", "Iniciar", onStart)
    }
}

@Composable
private fun ProfileScreen(
    modifier: Modifier,
    profile: UserProfile,
    waterMl: Int,
    onEdit: () -> Unit,
    onWater: () -> Unit,
    onProgress: () -> Unit
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar()
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.name, color = CeaColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Objetivo: ${profile.objective}", color = CeaColors.Muted, fontSize = 10.sp)
            }
            GhostAction("Editar perfil", onEdit)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("18", "Concluidos", Modifier.weight(1f))
            MetricCard("24", "Dias ativos", Modifier.weight(1f))
            MetricCard("12h40", "Tempo total", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("7 dias", "Sequencia", Modifier.weight(1f))
            MetricCard(profile.objective, "Mais treinado", Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        ProfileCalendarCard()
        Spacer(Modifier.height(18.dp))
        CeaCard {
            SectionTitle("Hidratacao")
            Text("$waterMl ml de 2500 ml", color = CeaColors.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (waterMl / 2500f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = CeaColors.Green,
                trackColor = CeaColors.CardAlt
            )
            Spacer(Modifier.height(12.dp))
            PrimaryAction("+ 250 ml", Modifier.fillMaxWidth(), onWater)
        }
        Spacer(Modifier.height(18.dp))
        SectionTitle("Historico recente")
        Spacer(Modifier.height(8.dp))
        WorkoutRow("Upper body forca", "Hoje - 42 min", "OK", onProgress)
        Spacer(Modifier.height(8.dp))
        WorkoutRow("Cardio HIIT", "Ontem - 28 min", "OK", onProgress)
        Spacer(Modifier.height(8.dp))
        WorkoutRow("Push hipertrofia A", "Segunda - 61 min", "OK", onProgress)
    }
}

@Composable
private fun ProfileCalendarCard() {
    var currentMonthOffset by remember { mutableIntStateOf(0) }

    val calendar = remember(currentMonthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JUNE)
            add(Calendar.MONTH, currentMonthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val monthName = remember(calendar) {
        calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR"))
            ?.replaceFirstChar { it.uppercase() } ?: ""
    }
    val year = remember(calendar) { calendar.get(Calendar.YEAR) }
    val totalDays = remember(calendar) { calendar.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val firstDayOfWeek = remember(calendar) { calendar.get(Calendar.DAY_OF_WEEK) }

    val startOffset = remember(firstDayOfWeek) {
        when (firstDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    val calendarCells = remember(startOffset, totalDays) {
        buildList {
            repeat(startOffset) { add(null) }
            for (day in 1..totalDays) { add(day) }
        }
    }

    CeaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionTitle("Frequencia de treinos")
                Spacer(Modifier.height(2.dp))
                Text("$monthName $year", color = CeaColors.Muted, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "<",
                    color = CeaColors.Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable { currentMonthOffset-- }
                        .padding(8.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = ">",
                    color = CeaColors.Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable { currentMonthOffset++ }
                        .padding(8.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("S", "T", "Q", "Q", "S", "S", "D").forEach {
                Text(
                    text = it,
                    color = CeaColors.Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        calendarCells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    if (day != null) {
                        val state = when (day) {
                            4, 10, 17, 20 -> DayState.Completed
                            8 -> DayState.Missed
                            12, 24 -> DayState.Scheduled
                            else -> DayState.Empty
                        }
                        CalendarCell(day, state)
                    } else {
                        Spacer(Modifier.size(36.dp))
                    }
                }
                if (week.size < 7) {
                    repeat(7 - week.size) {
                        Spacer(Modifier.size(36.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("Concluido", CeaColors.Green)
            StatusPill("Agendado", CeaColors.Blue)
            StatusPill("Perdido", CeaColors.Red)
        }
    }
}

@Composable
private fun ActiveWorkoutScreen(
    modifier: Modifier,
    workout: Workout,
    database: CeaDatabaseHelper,
    onFinished: () -> Unit
) {
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var isResting by remember { mutableStateOf(false) }
    var restTimeInput by remember { mutableStateOf("15") }
    var customRestSeconds by remember { mutableIntStateOf(15) }

    val exercises = workout.exercises
    val currentExercise = exercises.getOrNull(currentExerciseIndex) ?: ""

    val exercisesCatalog = remember { database.listExercises() }
    val currentExerciseObj = remember(currentExercise, exercisesCatalog) {
        exercisesCatalog.find { it.name.equals(currentExercise, ignoreCase = true) }
    }

    val defaultWorkSeconds = remember(currentExerciseObj) {
        when (currentExerciseObj?.level) {
            "Iniciante" -> 20
            "Intermediario" -> 25
            "Avancado" -> 30
            else -> 30
        }
    }

    var timeLeft by remember(currentExerciseIndex, isResting, defaultWorkSeconds) {
        mutableIntStateOf(if (isResting) customRestSeconds else defaultWorkSeconds)
    }
    var isTimerRunning by remember { mutableStateOf(true) }

    LaunchedEffect(isTimerRunning, timeLeft) {
        if (isTimerRunning && timeLeft > 0) {
            kotlinx.coroutines.delay(1000L)
            timeLeft--
        } else if (timeLeft == 0) {
            if (!isResting && currentExerciseIndex < exercises.size - 1) {
                isResting = true
            } else if (isResting) {
                isResting = false
                currentExerciseIndex++
            } else {
                database.logWorkoutCompletion(workout.id)
                onFinished()
            }
        }
    }

    Column(modifier) {
        Text(workout.title, color = CeaColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("${exercises.size} Exercícios em sequência", color = CeaColors.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        CeaCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isResting) "DESCANSO" else "EXECUTANDO",
                    color = if (isResting) CeaColors.Blue else CeaColors.Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                if (isResting) {
                    val nextExerciseName = exercises.getOrNull(currentExerciseIndex + 1) ?: "Fim"
                    Text(
                        text = "Próximo: $nextExerciseName",
                        color = CeaColors.Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    Text(
                        text = currentExercise,
                        color = CeaColors.Text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    if (currentExerciseObj != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(currentExerciseObj.level, CeaColors.Green)
                            StatusPill(currentExerciseObj.muscleGroup, CeaColors.Blue)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Instruções:",
                            color = CeaColors.Text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(Modifier.height(4.dp))
                        currentExerciseObj.instructions.split("\n").filter { it.isNotBlank() }.forEachIndexed { idx, step ->
                            Text(
                                text = "${idx + 1}. $step",
                                color = CeaColors.Muted,
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Start),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "${timeLeft}s",
                    color = CeaColors.Text,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { isTimerRunning = !isTimerRunning },
                        colors = ButtonDefaults.buttonColors(containerColor = CeaColors.CardAlt),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isTimerRunning) "Pausar" else "Retomar", color = CeaColors.Text)
                    }
                    if (!isResting) {
                        Button(
                            onClick = { timeLeft += 5 },
                            colors = ButtonDefaults.buttonColors(containerColor = CeaColors.CardAlt),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+5s", color = CeaColors.Green, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = {
                            if (!isResting && currentExerciseIndex < exercises.size - 1) {
                                isResting = true
                            } else {
                                isResting = false
                                if (currentExerciseIndex < exercises.size - 1) {
                                    currentExerciseIndex++
                                } else {
                                    database.logWorkoutCompletion(workout.id)
                                    onFinished()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CeaColors.CardAlt),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Pular", color = CeaColors.Text)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        CeaCard {
            SectionTitle("Configurar tempo de descanso")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CeaInput(
                    label = "Tempo de descanso (segundos)",
                    value = restTimeInput,
                    modifier = Modifier.weight(1f),
                    onValueChange = {
                        restTimeInput = it
                        it.toIntOrNull()?.let { s ->
                            if (s > 0) {
                                customRestSeconds = s
                            }
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionTitle("Exercícios do Treino")
        Spacer(Modifier.height(8.dp))
        exercises.forEachIndexed { index, name ->
            val isCurrent = index == currentExerciseIndex && !isResting
            val isCompleted = index < currentExerciseIndex

            CeaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .border(
                        width = if (isCurrent) 1.5.dp else 0.dp,
                        color = if (isCurrent) CeaColors.Green else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${index + 1}",
                            color = if (isCompleted) CeaColors.Green else CeaColors.Muted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = name,
                            color = if (isCurrent) CeaColors.Green else if (isCompleted) CeaColors.Muted else CeaColors.Text,
                            fontSize = 14.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (isCompleted) {
                        Text("✓ Concluído", color = CeaColors.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else if (isCurrent) {
                        Text("Executando...", color = CeaColors.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Aguardando", color = CeaColors.Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CeaCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CeaColors.Card),
        shape = RoundedCornerShape(12.dp),
        content = {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    )
}

@Composable
private fun CeaInput(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        label = { Text(label, color = CeaColors.Muted) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(9.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = CeaColors.Text,
            unfocusedTextColor = CeaColors.Text,
            focusedBorderColor = CeaColors.Green,
            unfocusedBorderColor = CeaColors.CardAlt,
            cursorColor = CeaColors.Green,
            focusedContainerColor = CeaColors.Black,
            unfocusedContainerColor = CeaColors.Black
        )
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = CeaColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun SelectableChips(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
                    .clickable { onSelected(option) },
                color = if (isSelected) CeaColors.Green else CeaColors.CardAlt,
                shape = RoundedCornerShape(999.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(
                        option,
                        color = if (isSelected) Color.Black else CeaColors.Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = CeaColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun PrimaryAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 46.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CeaColors.Green, contentColor = Color.Black),
        shape = RoundedCornerShape(9.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallAction(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 38.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CeaColors.Green, contentColor = Color.Black),
        shape = RoundedCornerShape(9.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GhostAction(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(9.dp)
    ) {
        Text(text, color = CeaColors.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    CeaCard(modifier) {
        Text(value, color = CeaColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = CeaColors.Muted, fontSize = 10.sp)
    }
}

@Composable
private fun ShortcutCard(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    CeaCard(
        modifier = modifier
            .height(92.dp)
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(icon, color = CeaColors.Green, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(label, color = CeaColors.Text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WorkoutRow(
    title: String,
    subtitle: String,
    action: String,
    onStart: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    CeaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = CeaColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = CeaColors.Muted, fontSize = 11.sp)

                if (onEdit != null || onDuplicate != null || onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (onEdit != null) {
                            Text(
                                text = "Editar",
                                color = CeaColors.Green,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(onClick = onEdit)
                            )
                        }
                        if (onDuplicate != null) {
                            Text(
                                text = "Duplicar",
                                color = CeaColors.Blue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(onClick = onDuplicate)
                            )
                        }
                        if (onDelete != null) {
                            Text(
                                text = "Excluir",
                                color = CeaColors.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(onClick = onDelete)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            SmallAction(action, onStart)
        }
    }
}

@Composable
private fun ExerciseRow(index: String, title: String, subtitle: String) {
    CeaCard(modifier = Modifier.padding(bottom = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CeaColors.CardAlt),
                contentAlignment = Alignment.Center
            ) {
                Text(index, color = CeaColors.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = CeaColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = CeaColors.Muted, fontSize = 10.sp)
            }
            Text(">", color = CeaColors.Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExerciseRow(index: String, exercise: Exercise, onClick: () -> Unit) {
    CeaCard(
        modifier = Modifier
            .padding(bottom = 9.dp)
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CeaColors.CardAlt),
                contentAlignment = Alignment.Center
            ) {
                Text(index, color = CeaColors.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(exercise.name, color = CeaColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = exercise.level,
                        color = CeaColors.Green,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                // Affected regions as chips (primary + secondary)
                val muscles = remember(exercise) {
                    val list = mutableListOf<String>()
                    if (exercise.primaryMuscles.isNotBlank()) {
                        list.addAll(exercise.primaryMuscles.split(",").map { it.trim().replaceFirstChar { c -> c.uppercase() } })
                    }
                    if (exercise.secondaryMuscles.isNotBlank()) {
                        list.addAll(exercise.secondaryMuscles.split(",").map { it.trim().replaceFirstChar { c -> c.uppercase() } })
                    }
                    list.distinct()
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    muscles.take(3).forEach { muscle ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CeaColors.CardAlt)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = muscle,
                                color = CeaColors.Muted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(">", color = CeaColors.Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExerciseDetailsDialog(
    exercise: Exercise,
    selectedExercises: MutableList<Exercise>,
    onDismiss: () -> Unit
) {
    val isAlreadyAdded = selectedExercises.any { it.name == exercise.name }

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

                // Add to Workout Button
                Button(
                    onClick = {
                        if (isAlreadyAdded) {
                            selectedExercises.removeAll { it.name == exercise.name }
                        } else {
                            selectedExercises.add(exercise)
                        }
                        onDismiss()
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

                // Primary muscles
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

                // Secondary muscles
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

@Composable
private fun DayBar(day: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height((value * 0.82f).dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (value >= 100) CeaColors.Green else CeaColors.CardAlt)
        )
        Spacer(Modifier.height(6.dp))
        Text(day, color = CeaColors.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProgressLine(label: String, value: Int, max: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
        Text(label, color = CeaColors.Muted, fontSize = 11.sp, modifier = Modifier.width(92.dp))
        LinearProgressIndicator(
            progress = { value / max.toFloat() },
            modifier = Modifier.weight(1f),
            color = CeaColors.Green,
            trackColor = CeaColors.CardAlt
        )
        Spacer(Modifier.width(8.dp))
        Text("$value kg", color = CeaColors.Text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalendarCell(day: Int, state: DayState) {
    val color = when (state) {
        DayState.Completed -> CeaColors.Green
        DayState.Scheduled -> CeaColors.Blue
        DayState.Missed -> CeaColors.Red
        DayState.Empty -> CeaColors.Card
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            day.toString(),
            color = if (state == DayState.Completed) Color.Black else CeaColors.Text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(text, color = CeaColors.Muted, fontSize = 10.sp)
    }
}

private fun String.firstName(): String {
    return trim().split(" ").firstOrNull().orEmpty().ifBlank { "Nome" }
}

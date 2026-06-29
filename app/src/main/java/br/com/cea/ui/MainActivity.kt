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
import br.com.cea.R
import br.com.cea.data.CeaDatabaseHelper
import br.com.cea.model.DayState
import br.com.cea.model.Exercise
import br.com.cea.model.UserProfile
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
    Profile("profile", "Perfil")
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
            Screen.ProfileSetup -> ""
        },
        subtitle = when (screen) {
            Screen.Home -> "Pronto para evoluir hoje?"
            Screen.CreateWorkout -> "Monte um plano personalizado"
            Screen.Progress -> "Sua evolucao em numeros"
            Screen.Calendar -> "Junho 2026"
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
                recommendationService = recommendationService,
                onExercises = { screen = Screen.Exercises },
                onSave = { workout ->
                    database.saveWorkout(workout)
                    analytics.track(context, "workout_generated")
                    refresh++
                    screen = Screen.MyWorkouts
                }
            )
            Screen.MyWorkouts -> MyWorkoutsScreen(
                modifier = modifier,
                workouts = database.listWorkouts(publicOnly = false),
                onNew = { screen = Screen.CreateWorkout },
                onStart = { screen = Screen.Exercises }
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
            Screen.Progress -> ProgressScreen(modifier, profile, bmiService)
            Screen.Exercises -> ExercisesScreen(modifier, database.listExercises())
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
    recommendationService: WorkoutRecommendationService,
    onExercises: () -> Unit,
    onSave: (Workout) -> Unit
) {
    var name by remember { mutableStateOf("Push hipertrofia A") }
    var objective by remember { mutableStateOf(profile.objective) }
    var level by remember { mutableStateOf(profile.level) }
    var duration by remember { mutableStateOf("60 min") }
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
        SelectableChips(listOf("Peito", "Ombro", "Triceps"), muscle) { muscle = it }
        Spacer(Modifier.height(16.dp))
        PrimaryAction("+ Adicionar exercicio", Modifier.fillMaxWidth(), onExercises)
        Spacer(Modifier.height(18.dp))
        SectionTitle("Exercicios")
        Spacer(Modifier.height(8.dp))
        ExerciseRow("1", "Supino reto", "4 series - 8/10 reps - 60 kg")
        ExerciseRow("2", "Desenvolvimento", "3 series - 10 reps - 22 kg")
        ExerciseRow("3", "Triceps corda", "3 series - 12 reps")
        Spacer(Modifier.height(14.dp))
        PrimaryAction("Salvar treino", Modifier.fillMaxWidth()) {
            val base = recommendationService.recommend(profile.copy(objective = objective, level = level))
            onSave(
                base.copy(
                    title = name.ifBlank { base.title },
                    objective = objective,
                    level = level,
                    duration = duration,
                    publicWorkout = true
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
    onStart: () -> Unit
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
                subtitle = "${workout.objective} - ${workout.duration}$origin\nEditar  Duplicar  Excluir",
                action = "Iniciar",
                onClick = onStart
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
                    onClick = { onImport(workout) }
                )
                Spacer(Modifier.height(10.dp))
            }
    }
}

@Composable
private fun ProgressScreen(modifier: Modifier, profile: UserProfile, bmiService: BmiService) {
    val bmi = bmiService.calculate(profile.weightKg, profile.heightCm)
    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("18", "Concluidos", Modifier.weight(1f))
            MetricCard("24", "Dias ativos", Modifier.weight(1f))
            MetricCard("7", "Sequencia", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
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
private fun ExercisesScreen(modifier: Modifier, exercises: List<Exercise>) {
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("Peito") }
    val filtered = exercises.filter {
        it.muscleGroup == muscle && (query.isBlank() || it.name.contains(query, ignoreCase = true))
    }

    Column(modifier) {
        CeaInput("Buscar exercicio", query) { query = it }
        Spacer(Modifier.height(12.dp))
        SelectableChips(listOf("Peito", "Costas", "Pernas", "Ombros"), muscle) { muscle = it }
        Spacer(Modifier.height(14.dp))
        if (filtered.isEmpty()) {
            CeaCard {
                Text("Nenhum exercicio encontrado", color = CeaColors.Text, fontWeight = FontWeight.Bold)
                Text("Cadastre exercicios para este grupo muscular.", color = CeaColors.Muted, fontSize = 12.sp)
            }
        } else {
            filtered.forEachIndexed { index, exercise ->
                ExerciseRow((index + 1).toString(), exercise.name, "${exercise.muscleGroup} - ${exercise.instructions}")
            }
        }
    }
}

@Composable
private fun CalendarScreen(modifier: Modifier, onStart: () -> Unit) {
    val days = (1..30).map {
        val state = when (it) {
            4, 10, 17, 20 -> DayState.Completed
            8 -> DayState.Missed
            12, 24 -> DayState.Scheduled
            else -> DayState.Empty
        }
        it to state
    }

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("S", "T", "Q", "Q", "S", "S", "D").forEach {
                Text(it, color = CeaColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { (day, state) ->
                    CalendarCell(day, state)
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
private fun WorkoutRow(title: String, subtitle: String, action: String, onClick: () -> Unit) {
    CeaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = CeaColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = CeaColors.Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.width(14.dp))
            SmallAction(action, onClick)
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

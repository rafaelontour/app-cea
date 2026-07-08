package br.com.cea.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import br.com.cea.model.Exercise
import br.com.cea.model.ScheduledWorkout
import br.com.cea.model.UserProfile
import br.com.cea.model.Workout
import br.com.cea.model.WorkoutExerciseSpec
import br.com.cea.model.WorkoutHistoryEntry
import br.com.cea.model.WeightLog
import org.json.JSONArray
import java.util.Calendar

class CeaDatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                age INTEGER,
                weight_kg REAL,
                height_cm REAL,
                activity_level TEXT,
                level TEXT,
                objective TEXT,
                frequency_per_week INTEGER,
                hours_per_day REAL,
                public_profile INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE workouts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                title TEXT,
                objective TEXT,
                level TEXT,
                duration TEXT,
                is_public INTEGER DEFAULT 0,
                is_imported INTEGER DEFAULT 0,
                origin_workout_id INTEGER,
                origin_user_name TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE workout_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                workout_id INTEGER,
                exercise_name TEXT,
                sets INTEGER,
                reps TEXT,
                duration_seconds INTEGER,
                rest_seconds INTEGER,
                order_index INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE exercise_catalog (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                muscle_group TEXT,
                level TEXT,
                instructions TEXT,
                image_uri TEXT,
                primary_muscles TEXT,
                secondary_muscles TEXT,
                equipment TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE TABLE workout_history (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, workout_id INTEGER, completed_at INTEGER, duration_seconds INTEGER, notes TEXT)")
        db.execSQL("CREATE TABLE scheduled_workouts (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, workout_id INTEGER, scheduled_at INTEGER, status TEXT)")
        db.execSQL("CREATE TABLE weekly_goals (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, week_start INTEGER, target_sessions INTEGER, completed_sessions INTEGER)")
        db.execSQL("CREATE TABLE achievements (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, code TEXT, title TEXT, earned_at INTEGER)")
        db.execSQL("CREATE TABLE weight_bmi_history (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, weight_kg REAL, height_cm REAL, bmi REAL, classification TEXT, recorded_at INTEGER)")
        db.execSQL("CREATE TABLE hydration_goals (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, daily_goal_ml INTEGER, reminder_enabled INTEGER, reminder_interval_minutes INTEGER)")
        db.execSQL("CREATE TABLE water_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, amount_ml INTEGER, logged_at INTEGER)")
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        listOf(
            "water_logs",
            "hydration_goals",
            "weight_bmi_history",
            "achievements",
            "weekly_goals",
            "scheduled_workouts",
            "workout_history",
            "exercise_catalog",
            "workout_exercises",
            "workouts",
            "users"
        ).forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }

    fun saveProfile(profile: UserProfile): Long {
        writableDatabase.delete("users", null, null)
        return writableDatabase.insert(
            "users",
            null,
            ContentValues().apply {
                put("name", profile.name)
                put("age", profile.age)
                put("weight_kg", profile.weightKg)
                put("height_cm", profile.heightCm)
                put("activity_level", profile.activityLevel)
                put("level", profile.level)
                put("objective", profile.objective)
                put("frequency_per_week", profile.frequencyPerWeek)
                put("hours_per_day", profile.hoursPerDay)
                put("public_profile", if (profile.publicProfile) 1 else 0)
            }
        )
    }

    fun getProfile(): UserProfile {
        readableDatabase.rawQuery("SELECT * FROM users LIMIT 1", null).use { cursor ->
            if (!cursor.moveToFirst()) return UserProfile()
            return UserProfile(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                age = cursor.getInt(cursor.getColumnIndexOrThrow("age")),
                weightKg = cursor.getDouble(cursor.getColumnIndexOrThrow("weight_kg")),
                heightCm = cursor.getDouble(cursor.getColumnIndexOrThrow("height_cm")),
                activityLevel = cursor.getString(cursor.getColumnIndexOrThrow("activity_level")),
                level = cursor.getString(cursor.getColumnIndexOrThrow("level")),
                objective = cursor.getString(cursor.getColumnIndexOrThrow("objective")),
                frequencyPerWeek = cursor.getInt(cursor.getColumnIndexOrThrow("frequency_per_week")),
                hoursPerDay = cursor.getDouble(cursor.getColumnIndexOrThrow("hours_per_day")),
                publicProfile = cursor.getInt(cursor.getColumnIndexOrThrow("public_profile")) == 1
            )
        }
    }

    fun saveWorkout(workout: Workout): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val workoutId = db.insert(
                "workouts",
                null,
                ContentValues().apply {
                    put("user_id", 1)
                    put("title", workout.title)
                    put("objective", workout.objective)
                    put("level", workout.level)
                    put("duration", workout.duration)
                    put("is_public", if (workout.publicWorkout) 1 else 0)
                    put("is_imported", if (workout.imported) 1 else 0)
                    put("origin_user_name", workout.origin)
                }
            )

            workout.exerciseSpecs.ifEmpty {
                workout.exercises.map { WorkoutExerciseSpec(name = it) }
            }.forEachIndexed { index, exercise ->
                db.insert(
                    "workout_exercises",
                    null,
                    ContentValues().apply {
                        put("workout_id", workoutId)
                        put("exercise_name", exercise.name)
                        put("sets", exercise.sets)
                        put("reps", exercise.reps)
                        put("duration_seconds", exercise.durationSeconds)
                        put("rest_seconds", exercise.restSeconds)
                        put("order_index", index)
                    }
                )
            }
            db.setTransactionSuccessful()
            return workoutId
        } finally {
            db.endTransaction()
        }
    }

    fun getWorkoutExercises(workoutId: Long): List<String> {
        return getWorkoutExerciseSpecs(workoutId).map { it.name }
    }

    fun getWorkoutExerciseSpecs(workoutId: Long): List<WorkoutExerciseSpec> {
        return buildList {
            readableDatabase.rawQuery(
                "SELECT exercise_name, sets, reps, duration_seconds, rest_seconds FROM workout_exercises WHERE workout_id = ? ORDER BY order_index",
                arrayOf(workoutId.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    add(
                        WorkoutExerciseSpec(
                            name = cursor.getString(0),
                            sets = cursor.getInt(1).coerceAtLeast(1),
                            reps = cursor.getString(2) ?: "10",
                            durationSeconds = cursor.getInt(3).coerceAtLeast(0),
                            restSeconds = cursor.getInt(4).coerceAtLeast(0)
                        )
                    )
                }
            }
        }
    }

    fun getWorkout(workoutId: Long): Workout? {
        readableDatabase.rawQuery("SELECT * FROM workouts WHERE id = ? LIMIT 1", arrayOf(workoutId.toString())).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return Workout(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                objective = cursor.getString(cursor.getColumnIndexOrThrow("objective")),
                level = cursor.getString(cursor.getColumnIndexOrThrow("level")),
                duration = cursor.getString(cursor.getColumnIndexOrThrow("duration")),
                publicWorkout = cursor.getInt(cursor.getColumnIndexOrThrow("is_public")) == 1,
                imported = cursor.getInt(cursor.getColumnIndexOrThrow("is_imported")) == 1,
                origin = cursor.getString(cursor.getColumnIndexOrThrow("origin_user_name")),
                exercises = getWorkoutExercises(workoutId),
                exerciseSpecs = getWorkoutExerciseSpecs(workoutId)
            )
        }
    }

    fun updateWorkout(workout: Workout) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.update(
                "workouts",
                ContentValues().apply {
                    put("title", workout.title)
                    put("objective", workout.objective)
                    put("level", workout.level)
                    put("duration", workout.duration)
                },
                "id = ?",
                arrayOf(workout.id.toString())
            )

            db.delete("workout_exercises", "workout_id = ?", arrayOf(workout.id.toString()))
            workout.exerciseSpecs.ifEmpty {
                workout.exercises.map { WorkoutExerciseSpec(name = it) }
            }.forEachIndexed { index, exercise ->
                db.insert(
                    "workout_exercises",
                    null,
                    ContentValues().apply {
                        put("workout_id", workout.id)
                        put("exercise_name", exercise.name)
                        put("sets", exercise.sets)
                        put("reps", exercise.reps)
                        put("duration_seconds", exercise.durationSeconds)
                        put("rest_seconds", exercise.restSeconds)
                        put("order_index", index)
                    }
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteWorkout(workoutId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            ensureScheduledWorkoutsTable()
            db.delete("scheduled_workouts", "workout_id = ?", arrayOf(workoutId.toString()))
            db.delete("workout_exercises", "workout_id = ?", arrayOf(workoutId.toString()))
            db.delete("workouts", "id = ?", arrayOf(workoutId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun duplicateWorkout(workoutId: Long) {
        val original = getWorkout(workoutId) ?: return

        val titleBase = original.title.replace(Regex("\\s\\d+$"), "")
        var count = 1
        var newTitle = "$titleBase $count"

        val db = readableDatabase
        while (true) {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM workouts WHERE title = ?", arrayOf(newTitle))
            val exists = cursor.use {
                if (it.moveToFirst()) it.getInt(0) > 0 else false
            }
            if (!exists) break
            count++
            newTitle = "$titleBase $count"
        }

        saveWorkout(original.copy(id = 0, title = newTitle))
    }

    fun listWorkouts(publicOnly: Boolean = false): List<Workout> {
        val sql = if (publicOnly) {
            "SELECT * FROM workouts WHERE is_public = 1 ORDER BY id DESC"
        } else {
            "SELECT * FROM workouts ORDER BY id DESC"
        }

        return buildList {
            readableDatabase.rawQuery(sql, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                    add(
                        Workout(
                            id = id,
                            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                            objective = cursor.getString(cursor.getColumnIndexOrThrow("objective")),
                            level = cursor.getString(cursor.getColumnIndexOrThrow("level")),
                            duration = cursor.getString(cursor.getColumnIndexOrThrow("duration")),
                            publicWorkout = cursor.getInt(cursor.getColumnIndexOrThrow("is_public")) == 1,
                            imported = cursor.getInt(cursor.getColumnIndexOrThrow("is_imported")) == 1,
                            origin = cursor.getString(cursor.getColumnIndexOrThrow("origin_user_name")),
                            exercises = getWorkoutExercises(id),
                            exerciseSpecs = getWorkoutExerciseSpecs(id)
                        )
                    )
                }
            }
        }
    }

    fun importWorkout(original: Workout, userName: String): Long {
        return saveWorkout(
            original.copy(
                id = 0,
                publicWorkout = false,
                imported = true,
                origin = userName
            )
        )
    }

    fun listExercises(): List<Exercise> {
        return buildList {
            readableDatabase.rawQuery("SELECT * FROM exercise_catalog ORDER BY id", null).use { cursor ->
                while (cursor.moveToNext()) {
                    add(
                        Exercise(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                            muscleGroup = cursor.getString(cursor.getColumnIndexOrThrow("muscle_group")),
                            level = cursor.getString(cursor.getColumnIndexOrThrow("level")),
                            instructions = cursor.getString(cursor.getColumnIndexOrThrow("instructions")),
                            imageUri = normalizeExerciseImageUris(cursor.getString(cursor.getColumnIndexOrThrow("image_uri"))),
                            primaryMuscles = cursor.getString(cursor.getColumnIndexOrThrow("primary_muscles")),
                            secondaryMuscles = cursor.getString(cursor.getColumnIndexOrThrow("secondary_muscles")),
                            equipment = cursor.getString(cursor.getColumnIndexOrThrow("equipment")) ?: ""
                        )
                    )
                }
            }
        }
    }

    fun logWater(amountMl: Int) {
        writableDatabase.insert(
            "water_logs",
            null,
            ContentValues().apply {
                put("user_id", 1)
                put("amount_ml", amountMl)
                put("logged_at", System.currentTimeMillis())
            }
        )
    }

    fun getTodayWaterMl(): Int {
        val start = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        readableDatabase.rawQuery(
            "SELECT SUM(amount_ml) FROM water_logs WHERE logged_at >= ?",
            arrayOf(start.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getInt(0)
        }
        return 0
    }

    private fun seed(db: SQLiteDatabase) {
        try {
            val jsonString = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val name = jsonObject.optString("name", "")
                val levelRaw = jsonObject.optString("level", "iniciante")
                val level = when (levelRaw.lowercase()) {
                    "iniciante" -> "Iniciante"
                    "intermediario" -> "Intermediário"
                    "avancado" -> "Avançado"
                    else -> "Iniciante"
                }

                val primaryMuscles = jsonObject.optJSONArray("primaryMuscles")
                val primaryMuscle = if (primaryMuscles != null && primaryMuscles.length() > 0) {
                    primaryMuscles.optString(0, "peito")
                } else {
                    "peito"
                }
                val muscleGroup = when (primaryMuscle.lowercase()) {
                    "peito" -> "Peito"
                    "dorsais", "meio-das-costas", "inferior-das-costas", "trapezio" -> "Costas"
                    "ombros" -> "Ombros"
                    "quadriceps", "isquiotibiais", "panturrilhas", "gluteos", "adutores", "abdutores" -> "Pernas"
                    "biceps", "triceps", "antebracos" -> "Braços"
                    "abdominais" -> "Abdominais"
                    else -> "Outros"
                }

                val primaryMusclesStr = buildList {
                    if (primaryMuscles != null) {
                        for (idx in 0 until primaryMuscles.length()) {
                            add(primaryMuscles.optString(idx))
                        }
                    }
                }.joinToString(",")

                val secondaryMusclesArray = jsonObject.optJSONArray("secondaryMuscles")
                val secondaryMusclesStr = buildList {
                    if (secondaryMusclesArray != null) {
                        for (idx in 0 until secondaryMusclesArray.length()) {
                            add(secondaryMusclesArray.optString(idx))
                        }
                    }
                }.joinToString(",")

                val instructionsArray = jsonObject.optJSONArray("instructions")
                val instructionsBuilder = StringBuilder()
                if (instructionsArray != null) {
                    for (j in 0 until instructionsArray.length()) {
                        instructionsBuilder.append(instructionsArray.optString(j))
                        if (j < instructionsArray.length() - 1) {
                            instructionsBuilder.append("\n")
                        }
                    }
                }
                val instructions = instructionsBuilder.toString()
                val imagesArray = jsonObject.optJSONArray("images")
                val imageUri = buildList {
                    if (imagesArray != null) {
                        for (idx in 0 until imagesArray.length()) {
                            add(imagesArray.optString(idx))
                        }
                    }
                }.filter { it.isNotBlank() }.flatMap { expandAssetImagePath(it) }.distinct().joinToString("|")
                val equipment = jsonObject.optString("equipment", "peso-do-corpo")

                insertExercise(db, name, muscleGroup, level, instructions, imageUri, primaryMusclesStr, secondaryMusclesStr, equipment)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback manual seeding
            insertExercise(db, "Supino reto", "Peito", "Intermediário", "Controle a descida e mantenha escápulas firmes.", "", "peito", "", "barra")
            insertExercise(db, "Crucifixo inclinado", "Peito", "Intermediário", "Abra os cotovelos com amplitude segura.", "", "peito", "", "halteres")
            insertExercise(db, "Flexão de braço", "Peito", "Iniciante", "Mantenha tronco alinhado durante todo o movimento.", "", "peito", "", "peso-do-corpo")
            insertExercise(db, "Puxada aberta", "Costas", "Intermediário", "Puxe com dorsais e evite elevar ombros.", "", "dorsais", "", "cabo")
            insertExercise(db, "Remada australiana", "Costas", "Iniciante", "Mantenha quadril firme e peito aberto.", "", "meio-das-costas", "", "peso-do-corpo")
            insertExercise(db, "Agachamento livre", "Pernas", "Iniciante", "Desça mantendo joelhos alinhados aos pés.", "", "quadriceps", "", "peso-do-corpo")
            insertExercise(db, "Afundo alternado", "Pernas", "Intermediário", "Controle a descida e suba sem impulso.", "", "quadriceps", "", "peso-do-corpo")
            insertExercise(db, "Elevação lateral", "Ombros", "Iniciante", "Suba até a linha dos ombros sem impulso.", "", "ombros", "", "halteres")
        }

        insertWorkout(db, "Push hipertrofia A", "Hipertrofia", "Intermediário", "60 min", true, false, null)
        insertWorkout(db, "Lower força", "Força", "Avançado", "50 min", true, false, null)
        insertWorkout(db, "Cardio HIIT", "Cardio", "Iniciante", "28 min", true, false, null)
        insertWorkout(db, "Mobilidade", "Mobilidade", "Iniciante", "20 min", true, false, null)
    }

    private fun insertExercise(db: SQLiteDatabase, name: String, muscle: String, level: String, instructions: String, imageUri: String, primaryMuscles: String, secondaryMuscles: String, equipment: String) {
        db.insert(
            "exercise_catalog",
            null,
            ContentValues().apply {
                put("name", name)
                put("muscle_group", muscle)
                put("level", level)
                put("instructions", instructions)
                put("image_uri", imageUri)
                put("primary_muscles", primaryMuscles)
                put("secondary_muscles", secondaryMuscles)
                put("equipment", equipment)
            }
        )
    }

    private fun normalizeExerciseImageUris(path: String): String {
        return path
            .split(",", "|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .flatMap { expandAssetImagePath(it) }
            .distinct()
            .joinToString("|")
    }

    private fun expandAssetImagePath(path: String): List<String> {
        val normalizedPath = when {
            path.startsWith("images/") -> path
            else -> "images/$path"
        }.trimEnd('/')
        val directory = if (isImageFile(normalizedPath)) {
            normalizedPath.substringBeforeLast("/", missingDelimiterValue = "")
        } else {
            normalizedPath
        }
        if (directory.isBlank()) return listOf(normalizedPath)

        val listedImages = try {
            context.assets.list(directory)
                ?.filter { isImageFile(it) }
                ?.sortedWith(compareBy<String> { it.substringBeforeLast(".").toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it })
                ?.map { "$directory/$it" }
                .orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

        return listedImages.ifEmpty { listOf(normalizedPath) }
    }

    private fun isImageFile(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
    }

    private fun insertWorkout(
        db: SQLiteDatabase,
        title: String,
        objective: String,
        level: String,
        duration: String,
        isPublic: Boolean,
        imported: Boolean,
        origin: String?
    ) {
        db.insert(
            "workouts",
            null,
            ContentValues().apply {
                put("user_id", 1)
                put("title", title)
                put("objective", objective)
                put("level", level)
                put("duration", duration)
                put("is_public", if (isPublic) 1 else 0)
                put("is_imported", if (imported) 1 else 0)
                put("origin_user_name", origin)
            }
        )
    }

    fun logWeight(weightKg: Double, heightCm: Double): Long {
        ensureWeightBmiHistoryTable()
        val bmi = if (heightCm > 0) weightKg / ((heightCm / 100.0) * (heightCm / 100.0)) else 0.0
        val classification = when {
            bmi < 18.5 -> "Abaixo do peso"
            bmi < 25.0 -> "Peso normal"
            bmi < 30.0 -> "Sobrepeso"
            else -> "Obesidade"
        }

        writableDatabase.execSQL("UPDATE users SET weight_kg = ?, height_cm = ?", arrayOf(weightKg, heightCm))

        return writableDatabase.insert(
            "weight_bmi_history",
            null,
            ContentValues().apply {
                put("user_id", 1)
                put("weight_kg", weightKg)
                put("height_cm", heightCm)
                put("bmi", bmi)
                put("classification", classification)
                put("recorded_at", System.currentTimeMillis())
            }
        )
    }

    private fun ensureWeightBmiHistoryTable() {
        writableDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS weight_bmi_history (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, weight_kg REAL, height_cm REAL, bmi REAL, classification TEXT, recorded_at INTEGER)"
        )
    }

    fun getWeightHistory(): List<WeightLog> {
        ensureWeightBmiHistoryTable()
        val format = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
        return buildList {
            readableDatabase.rawQuery(
                "SELECT id, weight_kg, recorded_at FROM weight_bmi_history ORDER BY recorded_at ASC",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val weight = cursor.getDouble(1)
                    val timestamp = cursor.getLong(2)
                    val dateStr = format.format(java.util.Date(timestamp))
                    add(WeightLog(id, weight, dateStr))
                }
            }
        }
    }

    fun scheduleWorkout(workoutId: Long, scheduledAt: Long): Long {
        ensureScheduledWorkoutsTable()
        findScheduledWorkoutOnDay(workoutId, scheduledAt)?.let { return it }
        return writableDatabase.insert(
            "scheduled_workouts",
            null,
            ContentValues().apply {
                put("user_id", 1)
                put("workout_id", workoutId)
                put("scheduled_at", scheduledAt)
                put("status", "scheduled")
            }
        )
    }

    fun rescheduleWorkout(scheduleId: Long, scheduledAt: Long): Int {
        ensureScheduledWorkoutsTable()
        val workoutId = getScheduledWorkoutId(scheduleId) ?: return 0
        if (findScheduledWorkoutOnDay(workoutId, scheduledAt, ignoredScheduleId = scheduleId) != null) {
            return 0
        }
        return writableDatabase.update(
            "scheduled_workouts",
            ContentValues().apply {
                put("scheduled_at", scheduledAt)
                put("status", "scheduled")
            },
            "id = ?",
            arrayOf(scheduleId.toString())
        )
    }

    fun cancelScheduledWorkout(scheduleId: Long): Int {
        ensureScheduledWorkoutsTable()
        return writableDatabase.delete("scheduled_workouts", "id = ?", arrayOf(scheduleId.toString()))
    }

    fun getScheduledWorkoutIdsForDay(timestamp: Long): Set<Long> {
        ensureScheduledWorkoutsTable()
        val (start, end) = dayRange(timestamp)
        val ids = mutableSetOf<Long>()
        readableDatabase.rawQuery(
            "SELECT workout_id FROM scheduled_workouts WHERE scheduled_at >= ? AND scheduled_at < ?",
            arrayOf(start.toString(), end.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0))
            }
        }
        return ids
    }

    fun getScheduledDaysInMonth(year: Int, month: Int): Set<Int> {
        ensureScheduledWorkoutsTable()
        val scheduledDays = mutableSetOf<Int>()
        val cal = Calendar.getInstance()
        readableDatabase.rawQuery("SELECT scheduled_at FROM scheduled_workouts", null).use { cursor ->
            while (cursor.moveToNext()) {
                cal.timeInMillis = cursor.getLong(0)
                if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                    scheduledDays.add(cal.get(Calendar.DAY_OF_MONTH))
                }
            }
        }
        return scheduledDays
    }

    fun getMissedScheduledDaysInMonth(year: Int, month: Int): Set<Int> {
        ensureScheduledWorkoutsTable()
        val completedDays = getCompletedDaysInMonth(year, month)
        val missedDays = mutableSetOf<Int>()
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance()
        readableDatabase.rawQuery("SELECT scheduled_at FROM scheduled_workouts", null).use { cursor ->
            while (cursor.moveToNext()) {
                cal.timeInMillis = cursor.getLong(0)
                if (cal.get(Calendar.YEAR) == year &&
                    cal.get(Calendar.MONTH) == month &&
                    cal.before(startOfToday(now)) &&
                    cal.get(Calendar.DAY_OF_MONTH) !in completedDays
                ) {
                    missedDays.add(cal.get(Calendar.DAY_OF_MONTH))
                }
            }
        }
        return missedDays
    }

    fun getUpcomingScheduledWorkouts(limit: Int = 5): List<ScheduledWorkout> {
        ensureScheduledWorkoutsTable()
        val start = startOfToday(Calendar.getInstance()).timeInMillis
        val sql = """
            SELECT s.id, s.workout_id, w.title, w.objective, w.duration, s.scheduled_at
            FROM scheduled_workouts s
            INNER JOIN workouts w ON s.workout_id = w.id
            WHERE s.scheduled_at >= ?
            ORDER BY s.scheduled_at ASC
            LIMIT ?
        """.trimIndent()
        return buildList {
            readableDatabase.rawQuery(sql, arrayOf(start.toString(), limit.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    add(
                        ScheduledWorkout(
                            id = cursor.getLong(0),
                            workoutId = cursor.getLong(1),
                            workoutTitle = cursor.getString(2),
                            workoutObjective = cursor.getString(3),
                            workoutDuration = cursor.getString(4),
                            scheduledAt = cursor.getLong(5)
                        )
                    )
                }
            }
        }
    }

    fun getScheduledWorkoutsForDay(timestamp: Long): List<ScheduledWorkout> {
        ensureScheduledWorkoutsTable()
        val (start, end) = dayRange(timestamp)
        val sql = """
            SELECT s.id, s.workout_id, w.title, w.objective, w.duration, s.scheduled_at
            FROM scheduled_workouts s
            INNER JOIN workouts w ON s.workout_id = w.id
            WHERE s.scheduled_at >= ? AND s.scheduled_at < ?
            ORDER BY s.scheduled_at ASC
        """.trimIndent()
        return buildList {
            readableDatabase.rawQuery(sql, arrayOf(start.toString(), end.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    add(
                        ScheduledWorkout(
                            id = cursor.getLong(0),
                            workoutId = cursor.getLong(1),
                            workoutTitle = cursor.getString(2),
                            workoutObjective = cursor.getString(3),
                            workoutDuration = cursor.getString(4),
                            scheduledAt = cursor.getLong(5)
                        )
                    )
                }
            }
        }
    }

    fun logWorkoutCompletion(workoutId: Long, durationSeconds: Int): Long {
        return writableDatabase.insert(
            "workout_history",
            null,
            ContentValues().apply {
                put("user_id", 1)
                put("workout_id", workoutId)
                put("completed_at", System.currentTimeMillis())
                put("duration_seconds", durationSeconds.coerceAtLeast(0))
                put("notes", "Treino concluído via temporizador")
            }
        )
    }

    private fun ensureScheduledWorkoutsTable() {
        writableDatabase.execSQL("CREATE TABLE IF NOT EXISTS scheduled_workouts (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, workout_id INTEGER, scheduled_at INTEGER, status TEXT)")
    }

    private fun findScheduledWorkoutOnDay(
        workoutId: Long,
        scheduledAt: Long,
        ignoredScheduleId: Long? = null
    ): Long? {
        val (start, end) = dayRange(scheduledAt)
        val ignoredClause = if (ignoredScheduleId != null) " AND id != ?" else ""
        val args = buildList {
            add(workoutId.toString())
            add(start.toString())
            add(end.toString())
            if (ignoredScheduleId != null) add(ignoredScheduleId.toString())
        }.toTypedArray()
        readableDatabase.rawQuery(
            "SELECT id FROM scheduled_workouts WHERE workout_id = ? AND scheduled_at >= ? AND scheduled_at < ?$ignoredClause LIMIT 1",
            args
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    private fun getScheduledWorkoutId(scheduleId: Long): Long? {
        readableDatabase.rawQuery(
            "SELECT workout_id FROM scheduled_workouts WHERE id = ? LIMIT 1",
            arrayOf(scheduleId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    private fun dayRange(timestamp: Long): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            timeInMillis = start.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return start.timeInMillis to end.timeInMillis
    }

    private fun startOfToday(now: Calendar): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun getCompletedWorkoutsCount(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM workout_history", null).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getInt(0)
            }
        }
        return 0
    }

    fun getActiveTrainingDaysCount(): Int {
        val activeDays = mutableSetOf<String>()
        val cal = Calendar.getInstance()
        readableDatabase.rawQuery("SELECT completed_at FROM workout_history", null).use { cursor ->
            while (cursor.moveToNext()) {
                cal.timeInMillis = cursor.getLong(0)
                activeDays.add("${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}")
            }
        }
        return activeDays.size
    }

    fun getCurrentWorkoutStreakDays(): Int {
        val completedDays = mutableSetOf<String>()
        val cal = Calendar.getInstance()
        readableDatabase.rawQuery("SELECT completed_at FROM workout_history", null).use { cursor ->
            while (cursor.moveToNext()) {
                cal.timeInMillis = cursor.getLong(0)
                completedDays.add("${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}")
            }
        }
        if (completedDays.isEmpty()) return 0

        val cursorDay = Calendar.getInstance()
        var key = "${cursorDay.get(Calendar.YEAR)}-${cursorDay.get(Calendar.DAY_OF_YEAR)}"
        if (key !in completedDays) {
            cursorDay.add(Calendar.DAY_OF_YEAR, -1)
            key = "${cursorDay.get(Calendar.YEAR)}-${cursorDay.get(Calendar.DAY_OF_YEAR)}"
        }

        var streak = 0
        while (key in completedDays) {
            streak++
            cursorDay.add(Calendar.DAY_OF_YEAR, -1)
            key = "${cursorDay.get(Calendar.YEAR)}-${cursorDay.get(Calendar.DAY_OF_YEAR)}"
        }
        return streak
    }

    fun getTotalWorkoutDurationSeconds(): Int {
        readableDatabase.rawQuery("SELECT COALESCE(SUM(duration_seconds), 0) FROM workout_history", null).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getInt(0)
            }
        }
        return 0
    }

    fun getCurrentWeekWorkoutCounts(): List<Int> {
        val counts = MutableList(7) { 0 }
        val weekStart = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekEnd = Calendar.getInstance().apply {
            timeInMillis = weekStart.timeInMillis
            add(Calendar.DAY_OF_YEAR, 7)
        }

        readableDatabase.rawQuery(
            "SELECT completed_at FROM workout_history WHERE completed_at >= ? AND completed_at < ?",
            arrayOf(weekStart.timeInMillis.toString(), weekEnd.timeInMillis.toString())
        ).use { cursor ->
            val cal = Calendar.getInstance()
            while (cursor.moveToNext()) {
                cal.timeInMillis = cursor.getLong(0)
                val index = when (cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }
                counts[index]++
            }
        }
        return counts
    }

    fun getMostTrainedObjective(): String? {
        val sql = """
            SELECT w.objective, COUNT(*) AS total
            FROM workout_history h
            INNER JOIN workouts w ON h.workout_id = w.id
            WHERE w.objective IS NOT NULL AND TRIM(w.objective) != ''
            GROUP BY w.objective
            ORDER BY total DESC, MAX(h.completed_at) DESC
            LIMIT 1
        """.trimIndent()
        readableDatabase.rawQuery(sql, null).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

    fun getWorkoutHistoryList(): List<WorkoutHistoryEntry> {
        val list = mutableListOf<WorkoutHistoryEntry>()
        val sql = """
            SELECT w.title, h.completed_at, h.duration_seconds
            FROM workout_history h
            LEFT JOIN workouts w ON h.workout_id = w.id
            ORDER BY h.completed_at DESC
        """.trimIndent()
        readableDatabase.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                val title = cursor.getString(0) ?: "Treino Livre"
                val completedAt = cursor.getLong(1)
                val durationSeconds = cursor.getInt(2)
                list.add(WorkoutHistoryEntry(title, completedAt, durationSeconds))
            }
        }
        return list
    }

    fun getCompletedDaysInMonth(year: Int, month: Int): Set<Int> {
        val completedDays = mutableSetOf<Int>()
        val cal = Calendar.getInstance()
        val sql = "SELECT completed_at FROM workout_history"
        readableDatabase.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                val timestamp = cursor.getLong(0)
                cal.timeInMillis = timestamp
                if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                    completedDays.add(cal.get(Calendar.DAY_OF_MONTH))
                }
            }
        }
        return completedDays
    }

    companion object {
        private const val DB_NAME = "cea.db"
        private const val DB_VERSION = 8
    }
}

package br.com.cea.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import br.com.cea.model.Exercise
import br.com.cea.model.UserProfile
import br.com.cea.model.Workout
import br.com.cea.model.WeightLog
import org.json.JSONArray

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
                secondary_muscles TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE TABLE workout_history (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, workout_id INTEGER, completed_at INTEGER, duration_seconds INTEGER, notes TEXT)")
        db.execSQL("CREATE TABLE weekly_goals (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, week_start INTEGER, target_sessions INTEGER, completed_sessions INTEGER)")
        db.execSQL("CREATE TABLE achievements (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, code TEXT, title TEXT, earned_at INTEGER)")
        db.execSQL("CREATE TABLE weight_bmi_history (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, weight_kg REAL, height_cm REAL, bmi REAL, classification TEXT, recorded_at INTEGER)")
        db.execSQL("CREATE TABLE hydration_goals (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, daily_goal_ml INTEGER, reminder_enabled INTEGER, reminder_interval_minutes INTEGER)")
        db.execSQL("CREATE TABLE water_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, amount_ml INTEGER, logged_at INTEGER)")
        db.execSQL("CREATE TABLE app_preferences (key TEXT PRIMARY KEY, value TEXT)")
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        listOf(
            "app_preferences",
            "water_logs",
            "hydration_goals",
            "weight_bmi_history",
            "achievements",
            "weekly_goals",
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

            workout.exercises.forEachIndexed { index, exerciseName ->
                db.insert(
                    "workout_exercises",
                    null,
                    ContentValues().apply {
                        put("workout_id", workoutId)
                        put("exercise_name", exerciseName)
                        put("sets", 3)
                        put("reps", "10")
                        put("duration_seconds", 0)
                        put("rest_seconds", 60)
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
        return buildList {
            readableDatabase.rawQuery(
                "SELECT exercise_name FROM workout_exercises WHERE workout_id = ? ORDER BY order_index",
                arrayOf(workoutId.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    add(cursor.getString(0))
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
                exercises = getWorkoutExercises(workoutId)
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
            workout.exercises.forEachIndexed { index, exerciseName ->
                db.insert(
                    "workout_exercises",
                    null,
                    ContentValues().apply {
                        put("workout_id", workout.id)
                        put("exercise_name", exerciseName)
                        put("sets", 3)
                        put("reps", "10")
                        put("duration_seconds", 0)
                        put("rest_seconds", 60)
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
                            exercises = getWorkoutExercises(id)
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
                            imageUri = cursor.getString(cursor.getColumnIndexOrThrow("image_uri")),
                            primaryMuscles = cursor.getString(cursor.getColumnIndexOrThrow("primary_muscles")),
                            secondaryMuscles = cursor.getString(cursor.getColumnIndexOrThrow("secondary_muscles"))
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
                    "intermediario" -> "Intermediario"
                    "avancado" -> "Avancado"
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
                val imageUri = jsonObject.optJSONArray("images")?.optString(0) ?: ""

                insertExercise(db, name, muscleGroup, level, instructions, imageUri, primaryMusclesStr, secondaryMusclesStr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback manual seeding
            insertExercise(db, "Supino reto", "Peito", "Intermediario", "Controle a descida e mantenha escapulas firmes.", "", "peito", "")
            insertExercise(db, "Crucifixo inclinado", "Peito", "Intermediario", "Abra os cotovelos com amplitude segura.", "", "peito", "")
            insertExercise(db, "Flexao de braco", "Peito", "Iniciante", "Mantenha tronco alinhado durante todo o movimento.", "", "peito", "")
            insertExercise(db, "Puxada aberta", "Costas", "Intermediario", "Puxe com dorsais e evite elevar ombros.", "", "dorsais", "")
            insertExercise(db, "Remada australiana", "Costas", "Iniciante", "Mantenha quadril firme e peito aberto.", "", "meio-das-costas", "")
            insertExercise(db, "Agachamento livre", "Pernas", "Iniciante", "Desca mantendo joelhos alinhados aos pes.", "", "quadriceps", "")
            insertExercise(db, "Afundo alternado", "Pernas", "Intermediario", "Controle a descida e suba sem impulso.", "", "quadriceps", "")
            insertExercise(db, "Elevacao lateral", "Ombros", "Iniciante", "Suba ate a linha dos ombros sem impulso.", "", "ombros", "")
        }

        insertWorkout(db, "Push hipertrofia A", "Hipertrofia", "Intermediario", "60 min", true, false, null)
        insertWorkout(db, "Lower forca", "Forca", "Avancado", "50 min", true, false, null)
        insertWorkout(db, "Cardio HIIT", "Cardio", "Iniciante", "28 min", true, false, null)
        insertWorkout(db, "Mobilidade", "Mobilidade", "Iniciante", "20 min", true, false, null)
    }

    private fun insertExercise(db: SQLiteDatabase, name: String, muscle: String, level: String, instructions: String, imageUri: String, primaryMuscles: String, secondaryMuscles: String) {
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
            }
        )
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
        val bmi = if (heightCm > 0) weightKg / ((heightCm / 100.0) * (heightCm / 100.0)) else 0.0
        val classification = when {
            bmi < 18.5 -> "Abaixo do peso"
            bmi < 25.0 -> "Peso normal"
            bmi < 30.0 -> "Sobrepeso"
            else -> "Obesidade"
        }

        writableDatabase.execSQL("UPDATE users SET weight_kg = ?", arrayOf(weightKg))

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

    fun getWeightHistory(): List<WeightLog> {
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

    companion object {
        private const val DB_NAME = "cea.db"
        private const val DB_VERSION = 5
    }
}

package br.com.cea.service

import android.content.Context
import android.os.Bundle
import android.util.Log
import br.com.cea.model.UserProfile
import br.com.cea.model.Workout
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class BmiService {
    fun calculate(weightKg: Double, heightCm: Double): Double {
        val meters = heightCm / 100.0
        if (weightKg <= 0 || meters <= 0) return 0.0
        return weightKg / (meters * meters)
    }

    fun classify(bmi: Double): String = when {
        bmi <= 0 -> "Dados invalidos"
        bmi < 18.5 -> "Abaixo do peso"
        bmi < 25 -> "Peso normal"
        bmi < 30 -> "Sobrepeso"
        bmi < 35 -> "Obesidade grau I"
        bmi < 40 -> "Obesidade grau II"
        else -> "Obesidade grau III"
    }
}

class WorkoutRecommendationService {
    fun recommend(profile: UserProfile): Workout {
        val title = when (profile.objective) {
            "Forca" -> "Upper body forca"
            "Cardio", "Emagrecimento" -> "Cardio HIIT"
            "Mobilidade" -> "Mobilidade e resistencia"
            else -> if (profile.level == "Iniciante") "Full body metabolico" else "Push hipertrofia A"
        }

        return Workout(
            title = title,
            objective = profile.objective,
            level = profile.level,
            duration = "${maxOf(30, (profile.hoursPerDay * 60).toInt())} min",
            publicWorkout = true,
            exercises = listOf(
                "Supino reto - 4 series - 8/10 reps",
                "Crucifixo inclinado - 3 series - 12 reps",
                "Flexao de braco - 3 series - ate a falha",
                "Agachamento livre - 4 series - 10 reps"
            )
        )
    }
}

class AnalyticsTracker {
    fun track(context: Context, eventName: String, params: Bundle = Bundle()) {
        Log.d("CEAAnalytics", "event=$eventName params=$params package=${context.packageName}")
    }
}

class ApiClient(private val baseUrl: String) {
    fun postJson(path: String, body: JSONObject): JSONObject {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }

        connection.outputStream.use { output: OutputStream ->
            output.write(body.toString().toByteArray(StandardCharsets.UTF_8))
        }

        val response = StringBuilder()
        BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                response.append(line)
                line = reader.readLine()
            }
        }
        return JSONObject(response.toString())
    }
}

package br.com.cea.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import br.com.cea.data.CeaDatabaseHelper
import br.com.cea.model.UserProfile
import br.com.cea.service.ProfileImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

@Composable
fun ProfileScreen(
    modifier: Modifier,
    profile: UserProfile,
    waterMl: Int,
    database: CeaDatabaseHelper,
    onEdit: () -> Unit,
    onWater: () -> Unit,
    onProgress: () -> Unit,
    onProfileChanged: (UserProfile) -> Unit = {}
) {
    var currentProfile by remember(profile) {
        mutableStateOf(profile)
    }

    var isProcessingImage by remember {
        mutableStateOf(false)
    }

    var showPhotoOptions by remember {
        mutableStateOf(false)
    }

    var showRemoveConfirmation by remember {
        mutableStateOf(false)
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    val profileImageStorage = remember(context) {
        ProfileImageStorage(context)
    }

    val completedCount = remember(currentProfile) {
        database.getCompletedWorkoutsCount()
    }

    val workoutHistory = remember(completedCount) {
        database.getWorkoutHistoryList()
    }

    val activeDays = remember(completedCount) {
        database.getActiveTrainingDaysCount()
    }

    val streakDays = remember(completedCount) {
        database.getCurrentWorkoutStreakDays()
    }

    val mostTrainedObjective = remember(completedCount) {
        database.getMostTrainedObjective() ?: "-"
    }

    fun showMessage(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    fun processSelectedImage(uri: android.net.Uri) {
        if (isProcessingImage) return

        coroutineScope.launch {
            isProcessingImage = true

            val previousImagePath = currentProfile.profileImagePath

            try {
                val updatedProfile = withContext(Dispatchers.IO) {
                    val newImagePath =
                        profileImageStorage.saveProfileImage(uri)

                    val profileWithNewImage = currentProfile.copy(
                        profileImagePath = newImagePath
                    )

                    try {
                        database.saveProfile(profileWithNewImage)
                    } catch (error: Exception) {
                        /*
                         * Se o banco falhar, removemos a imagem recém-criada
                         * e mantemos a foto anterior.
                         */
                        profileImageStorage.deleteProfileImage(
                            newImagePath
                        )

                        throw error
                    }

                    /*
                     * A foto antiga só é removida depois que o novo caminho
                     * foi salvo com sucesso no SQLite.
                     */
                    if (
                        !previousImagePath.isNullOrBlank() &&
                        previousImagePath != newImagePath
                    ) {
                        profileImageStorage.deleteProfileImage(
                            previousImagePath
                        )
                    }

                    profileWithNewImage
                }

                currentProfile = updatedProfile
                onProfileChanged(updatedProfile)

                showMessage(
                    if (previousImagePath.isNullOrBlank()) {
                        "Foto de perfil adicionada."
                    } else {
                        "Foto de perfil atualizada."
                    }
                )
            } catch (error: IOException) {
                showMessage(
                    error.message
                        ?: "Não foi possível utilizar essa imagem."
                )
            } catch (_: SecurityException) {
                showMessage(
                    "Não foi possível acessar a imagem selecionada."
                )
            } catch (_: Exception) {
                showMessage(
                    "Não foi possível salvar sua foto. Tente novamente."
                )
            } finally {
                isProcessingImage = false
            }
        }
    }

    fun removeProfileImage() {
        if (isProcessingImage) return

        val imagePathToRemove =
            currentProfile.profileImagePath

        if (imagePathToRemove.isNullOrBlank()) {
            showRemoveConfirmation = false
            return
        }

        coroutineScope.launch {
            isProcessingImage = true
            showRemoveConfirmation = false

            try {
                val updatedProfile = withContext(Dispatchers.IO) {
                    val profileWithoutImage = currentProfile.copy(
                        profileImagePath = null
                    )

                    /*
                     * Primeiro atualizamos o banco.
                     * Só depois removemos o arquivo físico.
                     */
                    database.saveProfile(profileWithoutImage)

                    profileImageStorage.deleteProfileImage(
                        imagePathToRemove
                    )

                    profileWithoutImage
                }

                currentProfile = updatedProfile
                onProfileChanged(updatedProfile)

                showMessage(
                    "Foto de perfil removida."
                )
            } catch (_: Exception) {
                showMessage(
                    "Não foi possível remover sua foto. Tente novamente."
                )
            } finally {
                isProcessingImage = false
            }
        }
    }

    val photoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { selectedUri ->
            /*
             * Quando o usuário cancela o Photo Picker,
             * selectedUri será null. Nesse caso, não fazemos nada
             * e não exibimos mensagem de erro.
             */
            if (selectedUri != null) {
                processSelectedImage(selectedUri)
            }
        }

    fun openPhotoPicker() {
        if (isProcessingImage) return

        showPhotoOptions = false

        photoPickerLauncher.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    fun handleAvatarClick() {
        if (isProcessingImage) return

        if (currentProfile.profileImagePath.isNullOrBlank()) {
            openPhotoPicker()
        } else {
            showPhotoOptions = true
        }
    }

    /*
     * Caso o caminho exista no banco, mas o arquivo interno tenha sido
     * apagado ou esteja ausente, limpamos o campo silenciosamente.
     *
     * O usuário verá as iniciais ou o botão de adicionar foto,
     * sem travamento e sem mensagem técnica.
     */
    LaunchedEffect(currentProfile.profileImagePath) {
        val currentImagePath =
            currentProfile.profileImagePath

        if (
            !currentImagePath.isNullOrBlank() &&
            !profileImageStorage.imageExists(currentImagePath)
        ) {
            val profileWithoutMissingImage =
                currentProfile.copy(
                    profileImagePath = null
                )

            runCatching {
                withContext(Dispatchers.IO) {
                    database.saveProfile(
                        profileWithoutMissingImage
                    )
                }
            }

            currentProfile = profileWithoutMissingImage
            onProfileChanged(profileWithoutMissingImage)
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserAvatar(
                        profileImagePath =
                            currentProfile.profileImagePath,
                        userName = currentProfile.name,
                        size = 72.dp,
                        editable = true,
                        showAddIconWhenEmpty =
                            currentProfile.profileImagePath.isNullOrBlank(),
                        onClick = {
                            handleAvatarClick()
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = if (
                            currentProfile.profileImagePath.isNullOrBlank()
                        ) {
                            "Adicionar foto"
                        } else {
                            "Alterar foto"
                        },
                        color = if (isProcessingImage) {
                            CeaColors.Muted
                        } else {
                            CeaColors.Green
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(
                            enabled = !isProcessingImage
                        ) {
                            handleAvatarClick()
                        }
                    )
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentProfile.name,
                        color = CeaColors.Text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Objetivo: ${currentProfile.objective}",
                        color = CeaColors.Muted,
                        fontSize = 10.sp
                    )
                }

                GhostAction(
                    text = "Editar perfil",
                    onClick = onEdit
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    value = completedCount.toString(),
                    label = "Concluidos",
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    value = activeDays.toString(),
                    label = "Dias ativos",
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    value = formatDays(streakDays),
                    label = "Sequencia",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            MetricCard(
                value = mostTrainedObjective,
                label = "Mais treinado"
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            CeaCard {
                SectionTitle(
                    text = "Hidratacao"
                )

                Text(
                    text = "$waterMl ml de " +
                            "${currentProfile.dailyWaterGoalMl} ml",
                    color = CeaColors.Muted,
                    fontSize = 12.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                LinearProgressIndicator(
                    progress = {
                        (
                                waterMl.toFloat() /
                                        currentProfile
                                            .dailyWaterGoalMl
                                            .toFloat()
                                ).coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = CeaColors.Green,
                    trackColor = CeaColors.CardAlt
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                PrimaryAction(
                    text = "+ 250 ml",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onWater
                )
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            SectionTitle(
                text = "Historico recente"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            if (workoutHistory.isEmpty()) {
                Text(
                    text = "Nenhum treino concluido recentemente.",
                    color = CeaColors.Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(
                        vertical = 8.dp
                    )
                )
            } else {
                workoutHistory.take(5).forEach { entry ->
                    val timestamp = entry.completedAt

                    val timeLabel = remember(timestamp) {
                        val difference =
                            System.currentTimeMillis() -
                                    timestamp

                        val days =
                            (
                                    difference /
                                            (1000 * 60 * 60 * 24)
                                    ).toInt()

                        when {
                            days == 0 -> "Hoje"
                            days == 1 -> "Ontem"
                            days < 7 -> "$days dias atras"

                            else -> {
                                val formatter =
                                    java.text.SimpleDateFormat(
                                        "dd/MM/yyyy",
                                        Locale.getDefault()
                                    )

                                formatter.format(
                                    java.util.Date(timestamp)
                                )
                            }
                        }
                    }

                    WorkoutRow(
                        title = entry.title,
                        subtitle = timeLabel,
                        action = "OK",
                        onStart = onProgress
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .zIndex(3f)
        )

        if (isProcessingImage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
                    .background(
                        Color.Black.copy(alpha = 0.55f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = CeaColors.Green
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Processando foto...",
                        color = CeaColors.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = {
                showPhotoOptions = false
            },
            title = {
                Text(
                    text = "Foto de perfil"
                )
            },
            text = {
                Text(
                    text = "Escolha o que deseja fazer com sua foto."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openPhotoPicker()
                    }
                ) {
                    Text(
                        text = "Alterar foto",
                        color = CeaColors.Green
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showPhotoOptions = false
                            showRemoveConfirmation = true
                        }
                    ) {
                        Text(
                            text = "Remover",
                            color = CeaColors.Red
                        )
                    }

                    TextButton(
                        onClick = {
                            showPhotoOptions = false
                        }
                    ) {
                        Text(
                            text = "Cancelar",
                            color = CeaColors.Muted
                        )
                    }
                }
            }
        )
    }

    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showRemoveConfirmation = false
            },
            title = {
                Text(
                    text = "Remover foto?"
                )
            },
            text = {
                Text(
                    text = "Sua foto de perfil será removida. " +
                            "Você poderá adicionar outra quando quiser."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        removeProfileImage()
                    }
                ) {
                    Text(
                        text = "Remover",
                        color = CeaColors.Red
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirmation = false
                    }
                ) {
                    Text(
                        text = "Cancelar",
                        color = CeaColors.Muted
                    )
                }
            }
        )
    }
}

private fun formatDays(days: Int): String {
    return if (days == 1) {
        "1 dia"
    } else {
        "$days dias"
    }
}
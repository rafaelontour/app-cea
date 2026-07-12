package br.com.cea.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cea.R
import br.com.cea.model.DayState
import br.com.cea.model.Exercise

@Composable
fun CeaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CeaColors.Card
        ),
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
fun CeaInput(
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
        label = {
            Text(
                text = label,
                color = CeaColors.Muted
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
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
fun FieldLabel(text: String) {
    Text(
        text = text,
        color = CeaColors.Muted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(
        modifier = Modifier.height(7.dp)
    )
}

@Composable
fun SelectableChips(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { option ->
            val isSelected = option == selected

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
                    .clickable {
                        onSelected(option)
                    },
                color = if (isSelected) {
                    CeaColors.Green
                } else {
                    CeaColors.CardAlt
                },
                shape = RoundedCornerShape(999.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 8.dp
                    )
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) {
                            Color.Black
                        } else {
                            CeaColors.Muted
                        },
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
fun SectionTitle(text: String) {
    Text(
        text = text,
        color = CeaColors.Text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun CeaLogoMark(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(
            id = R.drawable.cea_logo
        ),
        contentDescription = "Logo CEA",
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(
            RoundedCornerShape(14.dp)
        )
    )
}

@Composable
fun CeaBrandLockup(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CeaLogoMark(
            modifier = Modifier.size(58.dp)
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Column {
            Text(
                text = "CEA",
                color = CeaColors.Text,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Calisthenics Exercise Analysis",
                color = CeaColors.Muted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun PrimaryAction(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(
            min = 46.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = CeaColors.Green,
            contentColor = Color.Black,
            disabledContainerColor = CeaColors.CardAlt,
            disabledContentColor = CeaColors.Muted
        ),
        shape = RoundedCornerShape(9.dp),
        contentPadding = PaddingValues(
            horizontal = 18.dp,
            vertical = 10.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SecondaryAction(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(
            min = 46.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = CeaColors.CardAlt,
            contentColor = CeaColors.Text,
            disabledContainerColor = CeaColors.CardAlt,
            disabledContentColor = CeaColors.Muted
        ),
        shape = RoundedCornerShape(9.dp),
        contentPadding = PaddingValues(
            horizontal = 18.dp,
            vertical = 10.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SmallAction(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.heightIn(
            min = 38.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = CeaColors.Green,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(9.dp),
        contentPadding = PaddingValues(
            horizontal = 14.dp,
            vertical = 8.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SmallSecondaryAction(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.heightIn(
            min = 38.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = CeaColors.CardAlt,
            contentColor = CeaColors.Text,
            disabledContainerColor = CeaColors.CardAlt,
            disabledContentColor = CeaColors.Muted
        ),
        shape = RoundedCornerShape(9.dp),
        contentPadding = PaddingValues(
            horizontal = 14.dp,
            vertical = 8.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GhostAction(
    text: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(
            min = 44.dp
        ),
        shape = RoundedCornerShape(9.dp)
    ) {
        Text(
            text = text,
            color = CeaColors.Text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    CeaCard(
        modifier = modifier
    ) {
        Text(
            text = value,
            color = CeaColors.Text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        Text(
            text = label,
            color = CeaColors.Muted,
            fontSize = 10.sp
        )
    }
}

@Composable
fun ShortcutCard(
    icon: String,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    CeaCard(
        modifier = modifier
            .height(92.dp)
            .clickable(
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = icon,
                    color = CeaColors.Green,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = label,
                    color = CeaColors.Text,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun WorkoutRow(
    title: String,
    subtitle: String,
    action: String,
    onStart: () -> Unit,
    actionEnabled: Boolean = true,
    actionDone: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    extraActions: List<Pair<String, () -> Unit>> = emptyList()
) {
    CeaCard {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = CeaColors.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    color = CeaColors.Muted,
                    fontSize = 11.sp
                )

                if (
                    onEdit != null ||
                    onDuplicate != null ||
                    onDelete != null ||
                    extraActions.isNotEmpty()
                ) {
                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (onEdit != null) {
                            Text(
                                text = "Editar",
                                color = CeaColors.Green,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(
                                    onClick = onEdit
                                )
                            )
                        }

                        if (onDuplicate != null) {
                            Text(
                                text = "Duplicar",
                                color = CeaColors.Blue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(
                                    onClick = onDuplicate
                                )
                            )
                        }

                        if (onDelete != null) {
                            Text(
                                text = "Excluir",
                                color = CeaColors.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(
                                    onClick = onDelete
                                )
                            )
                        }

                        extraActions.forEach { (label, action) ->
                            Text(
                                text = label,
                                color = CeaColors.Blue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(
                                    onClick = action
                                )
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            if (actionDone || !actionEnabled) {
                SmallSecondaryAction(
                    text = action,
                    enabled = false,
                    onClick = onStart
                )
            } else {
                SmallAction(
                    text = action,
                    onClick = onStart
                )
            }
        }
    }
}

@Composable
fun ExerciseRow(
    index: String,
    title: String,
    subtitle: String
) {
    CeaCard(
        modifier = Modifier.padding(
            bottom = 9.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CeaColors.CardAlt),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index,
                    color = CeaColors.Green,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = CeaColors.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    color = CeaColors.Muted,
                    fontSize = 10.sp
                )
            }

            Text(
                text = ">",
                color = CeaColors.Muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ExerciseRow(
    index: String,
    exercise: Exercise,
    onClick: () -> Unit
) {
    CeaCard(
        modifier = Modifier
            .padding(bottom = 9.dp)
            .clickable(
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CeaColors.CardAlt),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index,
                    color = CeaColors.Green,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = exercise.name,
                        color = CeaColors.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text(
                        text = exercise.level,
                        color = CeaColors.Green,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                val muscles = remember(exercise) {
                    val list = mutableListOf<String>()

                    if (exercise.primaryMuscles.isNotBlank()) {
                        list.addAll(
                            exercise.primaryMuscles
                                .split(",")
                                .map {
                                    it.trim().replaceFirstChar { character ->
                                        character.uppercase()
                                    }
                                }
                        )
                    }

                    if (exercise.secondaryMuscles.isNotBlank()) {
                        list.addAll(
                            exercise.secondaryMuscles
                                .split(",")
                                .map {
                                    it.trim().replaceFirstChar { character ->
                                        character.uppercase()
                                    }
                                }
                        )
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
                                .clip(
                                    RoundedCornerShape(6.dp)
                                )
                                .background(CeaColors.CardAlt)
                                .padding(
                                    horizontal = 6.dp,
                                    vertical = 2.dp
                                )
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

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = ">",
                color = CeaColors.Muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DayBar(
    day: String,
    value: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height((value * 0.82f).dp)
                .clip(
                    RoundedCornerShape(6.dp)
                )
                .background(
                    if (value >= 100) {
                        CeaColors.Green
                    } else {
                        CeaColors.CardAlt
                    }
                )
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = day,
            color = CeaColors.Muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProgressLine(
    label: String,
    value: Int,
    max: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(
            vertical = 5.dp
        )
    ) {
        Text(
            text = label,
            color = CeaColors.Muted,
            fontSize = 11.sp,
            modifier = Modifier.width(92.dp)
        )

        LinearProgressIndicator(
            progress = {
                value / max.toFloat()
            },
            modifier = Modifier.weight(1f),
            color = CeaColors.Green,
            trackColor = CeaColors.CardAlt
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Text(
            text = "$value kg",
            color = CeaColors.Text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CalendarCell(
    day: Int,
    state: DayState
) {
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
            text = day.toString(),
            color = if (state == DayState.Completed) {
                Color.Black
            } else {
                CeaColors.Text
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )

        Spacer(
            modifier = Modifier.width(5.dp)
        )

        Text(
            text = text,
            color = CeaColors.Muted,
            fontSize = 10.sp
        )
    }
}

fun String.firstName(): String {
    return trim()
        .split(" ")
        .firstOrNull()
        .orEmpty()
        .ifBlank {
            "Atleta"
        }
}

fun String.normalizedTrainingLevel(): String {
    return when (trim()) {
        "Intermediario",
        "IntermediÃ¡rio",
        "Intermediário" -> "Intermediário"

        "Avancado",
        "AvanÃ§ado",
        "Avançado" -> "Avançado"

        else -> "Iniciante"
    }
}

fun trainingLevelRank(level: String): Int {
    return when (level.normalizedTrainingLevel()) {
        "Intermediário" -> 2
        "Avançado" -> 3
        else -> 1
    }
}

fun isTrainingLevelAbove(
    candidate: String,
    current: String
): Boolean {
    return trainingLevelRank(candidate) >
            trainingLevelRank(current)
}

/*
 * Este componente representa a marca do aplicativo.
 *
 * Ele é usado no cabeçalho superior e deve continuar exibindo
 * a logo do CEA. Não deve ser substituído pela foto do usuário.
 */
@Composable
fun Avatar() {
    CeaLogoMark(
        modifier = Modifier.size(38.dp)
    )
}

/*
 * Este componente representa exclusivamente o usuário.
 *
 * Ele poderá exibir:
 * - a foto salva no armazenamento interno;
 * - as iniciais do nome;
 * - um sinal de adição quando usado no perfil sem foto;
 * - um pequeno indicador de edição.
 */
@Composable
fun UserAvatar(
    profileImagePath: String?,
    userName: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    editable: Boolean = false,
    showAddIconWhenEmpty: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val profileBitmap = remember(profileImagePath) {
        if (profileImagePath.isNullOrBlank()) {
            null
        } else {
            runCatching {
                BitmapFactory.decodeFile(
                    profileImagePath
                )
            }.getOrNull()
        }
    }

    val initials = remember(userName) {
        generateUserInitials(userName)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(CeaColors.CardAlt)
                .border(
                    width = 1.dp,
                    color = CeaColors.Muted.copy(
                        alpha = 0.45f
                    ),
                    shape = CircleShape
                )
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (profileBitmap != null) {
                Image(
                    bitmap = profileBitmap.asImageBitmap(),
                    contentDescription = if (userName.isBlank()) {
                        "Foto de perfil"
                    } else {
                        "Foto de perfil de $userName"
                    },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else if (showAddIconWhenEmpty) {
                Text(
                    text = "+",
                    color = CeaColors.Green,
                    fontSize = (size.value * 0.42f).sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = initials,
                    color = CeaColors.Text,
                    fontSize = (size.value * 0.30f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        if (editable) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.32f)
                    .clip(CircleShape)
                    .background(CeaColors.Green)
                    .border(
                        width = 2.dp,
                        color = CeaColors.Black,
                        shape = CircleShape
                    )
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(
                                onClick = onClick
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = Color.Black,
                    fontSize = (size.value * 0.22f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun generateUserInitials(
    userName: String
): String {
    val nameParts = userName
        .trim()
        .split(
            Regex("\\s+")
        )
        .filter {
            it.isNotBlank()
        }

    if (nameParts.isEmpty()) {
        return "?"
    }

    if (nameParts.size == 1) {
        return nameParts
            .first()
            .take(1)
            .uppercase()
    }

    return buildString {
        append(
            nameParts
                .first()
                .first()
                .uppercaseChar()
        )

        append(
            nameParts
                .last()
                .first()
                .uppercaseChar()
        )
    }
}
package br.com.cea.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cea.model.DayState
import br.com.cea.model.Exercise

@Composable
fun CeaCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
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
fun FieldLabel(text: String) {
    Text(text, color = CeaColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(7.dp))
}

@Composable
fun SelectableChips(options: List<String>, selected: String, onSelected: (String) -> Unit) {
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
fun SectionTitle(text: String) {
    Text(text, color = CeaColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun PrimaryAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
fun SmallAction(text: String, onClick: () -> Unit) {
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
fun GhostAction(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(9.dp)
    ) {
        Text(text, color = CeaColors.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    CeaCard(modifier) {
        Text(value, color = CeaColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = CeaColors.Muted, fontSize = 10.sp)
    }
}

@Composable
fun ShortcutCard(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
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
fun WorkoutRow(
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
fun ExerciseRow(index: String, title: String, subtitle: String) {
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
fun ExerciseRow(index: String, exercise: Exercise, onClick: () -> Unit) {
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
fun DayBar(day: String, value: Int) {
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
fun ProgressLine(label: String, value: Int, max: Int) {
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
fun CalendarCell(day: Int, state: DayState) {
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
fun StatusPill(text: String, color: Color) {
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

fun String.firstName(): String {
    return trim().split(" ").firstOrNull().orEmpty().ifBlank { "Nome" }
}

@Composable
fun Avatar() {
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

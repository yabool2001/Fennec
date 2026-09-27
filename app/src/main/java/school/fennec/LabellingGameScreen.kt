package school.fennec

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabellingGameScreen(
    onBackToChapter: () -> Unit,
    onEndLearning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isImeVisible = WindowInsets.isImeVisible

    var guessedItems by remember { mutableStateOf(setOf<Int>()) }
    var lastGuessedItem by remember { mutableStateOf<Int?>(null) }
    var inputText by remember { mutableStateOf("") }

    val parts = listOf(
        PartData(0, "dendryty", listOf("dendryty", "dendryt"), 230f, 500f, R.drawable.bio_6_ne_r1_p_neuron_dendryty),
        PartData(1, "ciało komórki", listOf("ciało", "ciało komórki"), 140f, 240f, R.drawable.bio_6_ne_r1_p_neuron_cialo),
        PartData(2, "akson", listOf("akson"), 650f, 130f, R.drawable.bio_6_ne_r1_p_neuron_akson),
        PartData(3, "osłonka", listOf("osłonka", "osłonka mielinowa"), 600f, 310f, R.drawable.bio_6_ne_r1_p_neuron_oslonka)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            delay(100)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isImeVisible) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Biologia", fontWeight = FontWeight.Bold)
                        Text("Klasa: 6", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Progres: 50%")
                        Text("Rozdział: Królestwo zwierząt")
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var isStarred by remember { mutableStateOf(false) }
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Ulubione",
                            tint = if (isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { isStarred = !isStarred }
                        )
                        Text("Lekcja: Budowa komórki nerwowej", fontWeight = FontWeight.SemiBold)
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 1..11) {
                            val label = "L${i.toString().padStart(2, '0')}"
                            val isCurrent = i == 1
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        if (isCurrent) 2.dp else 0.dp,
                                        if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(819f / 596f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F3EC)),
            shape = RoundedCornerShape(16.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = maxWidth
                val canvasHeight = maxHeight

                parts.forEach { part ->
                    val isGuessed = guessedItems.contains(part.id)
                    val isLastGuessed = lastGuessedItem == part.id

                    val currentScale = if (isLastGuessed) scale else 1.0f
                    val currentAlpha = if (isLastGuessed) pulseAlpha else if (isGuessed) 1.0f else 0.3f

                    Image(
                        painter = painterResource(id = part.imageRes),
                        contentDescription = part.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(currentScale)
                            .alpha(currentAlpha),
                        contentScale = ContentScale.Fit
                    )

                    val labelOffsetX = canvasWidth * (part.pxX / 819f)
                    val labelOffsetY = canvasHeight * (part.pxY / 596f)

                    Box(
                        modifier = Modifier
                            .offset(labelOffsetX, labelOffsetY)
                            .scale(currentScale)
                            .alpha(currentAlpha)
                            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                            .border(1.5.dp, if (isGuessed) Color(0xFF001F3F) else Color.Black, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isGuessed) "✓ ${part.name.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }}" else "???",
                            color = if (isGuessed) Color(0xFF001F3F) else Color(0xFF000000),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalParts = parts.size
            for (i in 0 until totalParts) {
                val isFilled = i < guessedItems.size
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(
                            color = if (isFilled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = { newValue ->
                inputText = newValue
                val match = parts.find { !guessedItems.contains(it.id) && it.acceptedWords.any { word -> word.equals(newValue.trim(), ignoreCase = true) } }
                if (match != null) {
                    guessedItems = guessedItems + match.id
                    lastGuessedItem = match.id
                    inputText = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Wpisz część neuronu") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            visualTransformation = VisualTransformation.None,
            singleLine = true
        )

        if (!isImeVisible) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { }, modifier = Modifier.weight(1f)) {
                        Text("Poprzednia lekcja", textAlign = TextAlign.Center)
                    }
                    Button(onClick = { }, modifier = Modifier.weight(1f)) {
                        Text("Następna lekcja", textAlign = TextAlign.Center)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBackToChapter, 
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Wróć do rozdziału", textAlign = TextAlign.Center)
                    }
                    Button(
                        onClick = onEndLearning, 
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Koniec nauki", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

data class PartData(
    val id: Int,
    val name: String,
    val acceptedWords: List<String>,
    val pxX: Float,
    val pxY: Float,
    val imageRes: Int
)

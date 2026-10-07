package school.fennec

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class QuestionData(
    val id: Int,
    val fullName: String,
    val answer: String,
    val acceptedAnswers: List<String>,
    val empty: String,
    val hint1: String,
    val hint2: String,
    var status: Int = 0,
    val pictureSrc: String,
    val pictureAlpha: Float,
    val labelPxX: Float,
    val labelPxY: Float,
    val pictureResId: Int?
)

data class LessonData(
    val id: Int,
    val title: String,
    val task: String,
    val workingAreaBackgroundColor: Color,
    val canvasWidth: Float,
    val canvasHeight: Float,
    val questions: List<QuestionData>
)

fun loadStatusColors(context: Context): Map<Int, Color> {
    val defaultMap = mapOf(
        0 to Color.Transparent,
        1 to Color(0xFFE57373),
        2 to Color(0xFFFFB74D),
        3 to Color(0xFF4CAF50)
    )

    val jsonString = try {
        context.assets.open("ustawienia.json").bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        return defaultMap
    }

    return try {
        val obj = JSONObject(jsonString)
        val array = obj.optJSONArray("status") ?: return defaultMap
        val map = mutableMapOf<Int, Color>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val valNum = item.optInt("value", -1)
            val colorStr = item.optString("color", "")
            if (valNum != -1 && colorStr.isNotEmpty()) {
                val parsedColor = if (colorStr.equals("transparent", ignoreCase = true)) {
                    Color.Transparent
                } else {
                    try {
                        Color(android.graphics.Color.parseColor(colorStr))
                    } catch (e: Exception) {
                        Color.Transparent
                    }
                }
                map[valNum] = parsedColor
            }
        }
        if (map.isEmpty()) defaultMap else map
    } catch (e: Exception) {
        defaultMap
    }
}

fun calculateChapterProgress(allLessons: List<LessonData>): Int {
    var totalQuestions = 0
    var totalStatusSum = 0
    for (lesson in allLessons) {
        for (q in lesson.questions) {
            totalQuestions++
            totalStatusSum += q.status
        }
    }
    val maxPossibleSum = totalQuestions * 5
    if (maxPossibleSum == 0) return 0
    val percent = Math.floor((totalStatusSum.toDouble() / maxPossibleSum) * 100.0).toInt()
    return percent.coerceIn(0, 100)
}

fun loadLessons(context: Context, databaseFilename: String): List<LessonData> {
    val filename = if (databaseFilename.isNotEmpty()) databaseFilename else "bio_lekcje.json"

    val assetsJsonString = try {
        context.assets.open(filename).bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        return emptyList()
    }

    val savedStatuses = mutableMapOf<Pair<Int, Int>, Int>()
    val savedFile = File(context.filesDir, filename)
    if (savedFile.exists()) {
        try {
            val savedJsonArray = JSONArray(savedFile.readText())
            for (i in 0 until savedJsonArray.length()) {
                val lessonObj = savedJsonArray.optJSONObject(i) ?: continue
                val lId = lessonObj.optInt("id", i + 1)
                val qArray = lessonObj.optJSONArray("questions") ?: lessonObj.optJSONArray("answers")
                if (qArray != null) {
                    for (j in 0 until qArray.length()) {
                        val qObj = qArray.optJSONObject(j) ?: continue
                        val qId = qObj.optInt("id", j + 1)
                        val st = qObj.optInt("status", 0)
                        savedStatuses[Pair(lId, qId)] = st
                    }
                }
            }
        } catch (_: Exception) {}
    }

    val jsonArray = JSONArray(assetsJsonString)
    val lessons = mutableListOf<LessonData>()

    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.optJSONObject(i) ?: continue
        val id = obj.optInt("id", i + 1)
        val title = obj.optString("title", "Lekcja $id")
        val task = obj.optString("task", "Wpisz odpowiedź")
        
        val hexColorStr = obj.optString("working_area_background_color", "#F7F3EC")
        val bgHex = if (hexColorStr.startsWith("0x") || hexColorStr.startsWith("0X")) {
            "#" + hexColorStr.substring(2)
        } else if (!hexColorStr.startsWith("#")) {
            "#$hexColorStr"
        } else hexColorStr
        
        val parsedColor = try {
            Color(android.graphics.Color.parseColor(bgHex))
        } catch (e: Exception) {
            Color(0xFFF7F3EC)
        }

        val canvasWidth = obj.optDouble("canvas_width", 819.0).toFloat()
        val canvasHeight = obj.optDouble("canvas_height", 596.0).toFloat()

        val questionsArray = obj.optJSONArray("questions") ?: obj.optJSONArray("answers")
        val questions = mutableListOf<QuestionData>()
        if (questionsArray != null) {
            for (j in 0 until questionsArray.length()) {
                val qObj = questionsArray.optJSONObject(j) ?: continue
                val qId = qObj.optInt("id", j + 1)
                val fullName = qObj.optString("full_name", "")
                val answer = qObj.optString("answer", fullName)
                
                val acceptedAnswersList = mutableListOf<String>()
                val acceptedArray = qObj.optJSONArray("accepted_answers")
                if (acceptedArray != null) {
                    for (k in 0 until acceptedArray.length()) {
                        val word = acceptedArray.optString(k, "")
                        if (word.isNotEmpty()) acceptedAnswersList.add(word)
                    }
                }
                if (acceptedAnswersList.isEmpty() && answer.isNotEmpty()) {
                    acceptedAnswersList.add(answer)
                }

                val emptyStr = qObj.optString("empty", "???")
                val hint1Str = qObj.optString("hint1", emptyStr)
                val hint2Str = qObj.optString("hint2", hint1Str)
                val statusVal = savedStatuses[Pair(id, qId)] ?: qObj.optInt("status", 0)

                val pictureSrc = qObj.optString("picture_src", "")
                val pictureAlpha = qObj.optDouble("picture_alpha", 0.3).toFloat()

                val labelPxX = qObj.optDouble("label_px_x", 0.0).toFloat()
                val labelPxY = qObj.optDouble("label_px_y", 0.0).toFloat()

                val pictureResId = if (pictureSrc.isNotEmpty()) {
                    context.resources.getIdentifier(pictureSrc, "drawable", context.packageName)
                } else 0

                questions.add(
                    QuestionData(
                        id = qId,
                        fullName = fullName,
                        answer = answer,
                        acceptedAnswers = acceptedAnswersList,
                        empty = emptyStr,
                        hint1 = hint1Str,
                        hint2 = hint2Str,
                        status = statusVal,
                        pictureSrc = pictureSrc,
                        pictureAlpha = pictureAlpha,
                        labelPxX = labelPxX,
                        labelPxY = labelPxY,
                        pictureResId = if (pictureResId != 0) pictureResId else null
                    )
                )
            }
        }

        lessons.add(
            LessonData(
                id = id,
                title = title,
                task = task,
                workingAreaBackgroundColor = parsedColor,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                questions = questions
            )
        )
    }

    return lessons
}

fun saveQuestionStatus(
    context: Context,
    databaseFilename: String,
    lessonId: Int,
    questionId: Int,
    newStatus: Int
) {
    val filename = if (databaseFilename.isNotEmpty()) databaseFilename else "bio_lekcje.json"
    val file = File(context.filesDir, filename)

    val assetsJsonString = try {
        context.assets.open(filename).bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        return
    }

    try {
        val savedStatuses = mutableMapOf<Pair<Int, Int>, Int>()
        if (file.exists()) {
            try {
                val existingArr = JSONArray(file.readText())
                for (i in 0 until existingArr.length()) {
                    val lObj = existingArr.optJSONObject(i) ?: continue
                    val lId = lObj.optInt("id", i + 1)
                    val qArray = lObj.optJSONArray("questions") ?: lObj.optJSONArray("answers")
                    if (qArray != null) {
                        for (j in 0 until qArray.length()) {
                            val qObj = qArray.optJSONObject(j) ?: continue
                            val qId = qObj.optInt("id", j + 1)
                            val st = qObj.optInt("status", 0)
                            savedStatuses[Pair(lId, qId)] = st
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        savedStatuses[Pair(lessonId, questionId)] = newStatus

        val jsonArray = JSONArray(assetsJsonString)
        for (i in 0 until jsonArray.length()) {
            val lessonObj = jsonArray.optJSONObject(i) ?: continue
            val lId = lessonObj.optInt("id", i + 1)
            val questionsArray = lessonObj.optJSONArray("questions") ?: lessonObj.optJSONArray("answers")
            if (questionsArray != null) {
                for (j in 0 until questionsArray.length()) {
                    val qObj = questionsArray.optJSONObject(j) ?: continue
                    val qId = qObj.optInt("id", j + 1)
                    val st = savedStatuses[Pair(lId, qId)] ?: qObj.optInt("status", 0)
                    qObj.put("status", st)
                }
            }
        }
        file.writeText(jsonArray.toString(2))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabellingGameScreen(
    onBackToChapter: () -> Unit,
    onEndLearning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isImeVisible = WindowInsets.isImeVisible

    val selectedChapter = AppGlobalState.selectedChapter
    val selectedLevel = AppGlobalState.selectedLevel

    val isFundamenty = selectedLevel.equals("Fundamenty", ignoreCase = true)

    val databaseFilename = if (isFundamenty) {
        selectedChapter?.fundamentyDatabase ?: "bio_lekcje.json"
    } else {
        selectedChapter?.doskonalenieDatabase ?: "bio_lekcje.json"
    }

    val allowedLessonIds = if (isFundamenty) {
        selectedChapter?.fundamentyLessonIds ?: emptyList()
    } else {
        selectedChapter?.doskonalenieLessonIds ?: emptyList()
    }

    val statusColors = remember { loadStatusColors(context) }

    val rawLessons = remember(databaseFilename) {
        loadLessons(context, databaseFilename)
    }

    val allLessons = remember(rawLessons, allowedLessonIds) {
        if (allowedLessonIds.isNotEmpty()) {
            rawLessons.filter { lesson -> allowedLessonIds.contains(lesson.id) }
        } else {
            rawLessons
        }
    }

    var currentLessonIndex by remember(allLessons) {
        val firstUnmastered = allLessons.indexOfFirst { lesson ->
            lesson.questions.isEmpty() || !lesson.questions.all { it.status >= 5 }
        }
        mutableIntStateOf(if (firstUnmastered != -1) firstUnmastered else 0)
    }
    val currentLesson = allLessons.getOrNull(currentLessonIndex)

    val questions = currentLesson?.questions ?: emptyList()

    // Auto-detect mastered questions (status >= 5) so they are pre-revealed without pulsing
    val knownItemIds = remember(currentLesson) {
        questions.filter { it.status >= 5 }.map { it.id }.toSet()
    }

    var guessedItems by remember(currentLesson) { mutableStateOf(knownItemIds) }
    var lastGuessedItem by remember(currentLesson) { mutableStateOf<Int?>(null) }
    var inputText by remember(currentLesson) { mutableStateOf("") }
    var hintLevel by remember(currentLesson) { mutableIntStateOf(0) } // 0: empty, 1: hint1, 2: hint2

    val chapterProgress = remember(allLessons, guessedItems) {
        calculateChapterProgress(allLessons)
    }

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
            // Block 1 Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(AppGlobalState.selectedSubject, fontWeight = FontWeight.Bold)
                        Text("Klasa: ${AppGlobalState.selectedClass}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Progres: $chapterProgress%")
                        Text("Rozdział: ${selectedChapter?.title ?: "Rozdział 1"}")
                    }
                }
            }

            // Block 2 Card
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
                        Text("Lekcja: ${currentLesson?.title ?: ""}", fontWeight = FontWeight.SemiBold)
                    }

                    // Dynamic Lesson Tiles L01, L02...
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in allLessons.indices) {
                            val label = "L${(i + 1).toString().padStart(2, '0')}"
                            val isCurrent = i == currentLessonIndex
                            val lessonObj = allLessons[i]
                            val isMastered = lessonObj.questions.isNotEmpty() && lessonObj.questions.all { it.status >= 5 }

                            val tileBg = if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else if (isMastered) {
                                Color(0xFFC8E6C9)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }

                            val tileBorder = if (isCurrent) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else if (isMastered) {
                                BorderStroke(2.dp, Color(0xFF4CAF50))
                            } else null

                            val tileTextColor = if (isCurrent) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else if (isMastered) {
                                Color(0xFF1B5E20)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            val borderMod = if (tileBorder != null) Modifier.border(tileBorder, RoundedCornerShape(8.dp)) else Modifier

                            Box(
                                modifier = Modifier
                                    .background(tileBg, RoundedCornerShape(8.dp))
                                    .then(borderMod)
                                    .clickable { currentLessonIndex = i }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tileTextColor,
                                    fontWeight = if (isCurrent || isMastered) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Working Canvas Area
        val canvasWidthRatio = currentLesson?.canvasWidth ?: 819f
        val canvasHeightRatio = currentLesson?.canvasHeight ?: 596f
        val bgColor = currentLesson?.workingAreaBackgroundColor ?: Color(0xFFF7F3EC)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(canvasWidthRatio / canvasHeightRatio),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val containerWidth = maxWidth
                val containerHeight = maxHeight

                questions.forEach { question ->
                    val isGuessed = guessedItems.contains(question.id)
                    val isLastGuessed = lastGuessedItem == question.id

                    val currentScale = if (isLastGuessed) scale else 1.0f
                    val currentAlpha = if (isLastGuessed) pulseAlpha else if (isGuessed) 1.0f else question.pictureAlpha

                    if (question.pictureResId != null) {
                        Image(
                            painter = painterResource(id = question.pictureResId),
                            contentDescription = question.fullName,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(currentScale)
                                .alpha(currentAlpha),
                            contentScale = ContentScale.Fit
                        )
                    }

                    val labelOffsetX = containerWidth * (question.labelPxX / canvasWidthRatio)
                    val labelOffsetY = containerHeight * (question.labelPxY / canvasHeightRatio)

                    val labelText = if (isGuessed) {
                        val textToDisplay = if (question.fullName.isNotEmpty()) question.fullName else question.answer
                        "✓ ${textToDisplay.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }}"
                    } else {
                        when (hintLevel) {
                            0 -> question.empty
                            1 -> question.hint1
                            else -> question.hint2
                        }
                    }

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
                            text = labelText,
                            color = if (isGuessed) Color(0xFF001F3F) else Color(0xFF000000),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Segmented Progress Bar: 3 stacked vertical bars per question
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalParts = questions.size
            val color0 = statusColors[0] ?: Color.Transparent
            val color1 = statusColors[1] ?: Color(0xFFE57373)
            val color2 = statusColors[2] ?: Color(0xFFFFB74D)
            val color3 = statusColors[3] ?: Color(0xFF4CAF50)
            val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

            for (i in 0 until totalParts) {
                val q = questions.getOrNull(i)
                val qStatus = q?.status ?: 0

                val bottomColor = when {
                    qStatus == 1 -> color1
                    qStatus == 2 -> color2
                    qStatus >= 3 -> color3
                    else -> color0
                }

                val middleColor = when {
                    qStatus >= 4 -> color3
                    else -> color0
                }

                val topColor = when {
                    qStatus >= 5 -> color3
                    else -> color0
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar (Bar 3)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(topColor, RoundedCornerShape(2.dp))
                            .border(
                                width = 1.dp,
                                color = if (topColor == Color.Transparent) outlineColor else Color.Transparent,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                    // Middle Bar (Bar 2)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(middleColor, RoundedCornerShape(2.dp))
                            .border(
                                width = 1.dp,
                                color = if (middleColor == Color.Transparent) outlineColor else Color.Transparent,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                    // Bottom Bar (Bar 1)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(bottomColor, RoundedCornerShape(2.dp))
                            .border(
                                width = 1.dp,
                                color = if (bottomColor == Color.Transparent) outlineColor else Color.Transparent,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }

        // Hints Row with 2 Lightbulb lamps (visible ONLY when keyboard is open)
        if (isImeVisible) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (hintLevel < 2) hintLevel++
                    },
                    enabled = hintLevel < 2,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hintLevel < 2) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (hintLevel < 2) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = when (hintLevel) {
                            0 -> "Podpowiedź"
                            1 -> "Odkryj więcej"
                            else -> "Brak kolejnych podpowiedzi"
                        }
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = "Podpowiedź 1",
                        tint = if (hintLevel >= 1) Color(0xFFFFC107) else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = "Podpowiedź 2",
                        tint = if (hintLevel >= 2) Color(0xFFFFC107) else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Answer Input Field
        OutlinedTextField(
            value = inputText,
            onValueChange = { newValue ->
                inputText = newValue
                val match = questions.find { q ->
                    !guessedItems.contains(q.id) && q.acceptedAnswers.any { word ->
                        word.equals(newValue.trim(), ignoreCase = true)
                    }
                }
                if (match != null) {
                    guessedItems = guessedItems + match.id
                    lastGuessedItem = match.id

                    val oldStatus = match.status
                    val newStatus = when (hintLevel) {
                        2 -> 1
                        1 -> 2
                        else -> if (oldStatus < 3) 3 else (oldStatus + 1).coerceAtMost(5)
                    }
                    match.status = newStatus

                    if (currentLesson != null) {
                        saveQuestionStatus(
                            context = context,
                            databaseFilename = databaseFilename,
                            lessonId = currentLesson.id,
                            questionId = match.id,
                            newStatus = newStatus
                        )
                    }

                    inputText = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(currentLesson?.task ?: "Wpisz odpowiedź") },
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
                    Button(
                        onClick = {
                            val prevUnmastered = (currentLessonIndex - 1 downTo 0).firstOrNull { idx ->
                                !allLessons[idx].questions.all { it.status >= 5 }
                            }
                            if (prevUnmastered != null) {
                                currentLessonIndex = prevUnmastered
                            } else if (currentLessonIndex > 0) {
                                currentLessonIndex--
                            }
                        },
                        enabled = currentLessonIndex > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Poprzednia lekcja", textAlign = TextAlign.Center)
                    }
                    Button(
                        onClick = {
                            val nextUnmastered = (currentLessonIndex + 1 until allLessons.size).firstOrNull { idx ->
                                !allLessons[idx].questions.all { it.status >= 5 }
                            }
                            if (nextUnmastered != null) {
                                currentLessonIndex = nextUnmastered
                            } else if (currentLessonIndex < allLessons.size - 1) {
                                currentLessonIndex++
                            }
                        },
                        enabled = currentLessonIndex < allLessons.size - 1,
                        modifier = Modifier.weight(1f)
                    ) {
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

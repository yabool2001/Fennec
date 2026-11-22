package school.fennec

import android.content.Context
import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import android.speech.tts.TextToSpeech
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

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
    val labelPositions: List<Pair<Float, Float>>,
    val labelWidthPx: Float = 0f,
    val borderWidth: Float = 1.5f,
    val textUnderline: Int = 0,
    val textColor: Color? = null,
    val labelBackgroundColor: Color? = null,
    val labelAlpha: Float? = null,
    val textAlignment: String = "center",
    val textSizeSp: Float = 13f,
    val pictureResId: Int?,
    val audio: String = ""
)

data class DescriptionData(
    val id: Int,
    val text: String,
    val pictureSrc: String,
    val pictureAlpha: Float,
    val labelPositions: List<Pair<Float, Float>>,
    val labelWidthPx: Float = 0f,
    val borderWidth: Float = 1.5f,
    val textUnderline: Int = 0,
    val textColor: Color? = null,
    val labelBackgroundColor: Color? = null,
    val labelAlpha: Float? = null,
    val textAlignment: String = "center",
    val textSizeSp: Float = 13f,
    val pictureResId: Int?,
    val audio: String = ""
)

data class LessonData(
    val id: Int,
    val title: String,
    val task: String,
    val workingAreaBackgroundColor: Color,
    val canvasWidth: Float,
    val canvasHeight: Float,
    val questions: List<QuestionData>,
    val descriptions: List<DescriptionData> = emptyList(),
    val audio: String = ""
)

fun parseTextAlign(alignStr: String): TextAlign {
    return when (alignStr.lowercase()) {
        "left" -> TextAlign.Left
        "right" -> TextAlign.Right
        "justify" -> TextAlign.Justify
        else -> TextAlign.Center
    }
}

fun parseBoxAlignment(alignStr: String): Alignment {
    return when (alignStr.lowercase()) {
        "left" -> Alignment.CenterStart
        "right" -> Alignment.CenterEnd
        else -> Alignment.Center
    }
}

fun parseCoordinateList(obj: JSONObject, primaryKey: String, fallbackKey: String): List<Float> {
    val key = if (obj.has(primaryKey)) primaryKey else if (obj.has(fallbackKey)) fallbackKey else return listOf(0f)
    val array = obj.optJSONArray(key)
    if (array != null) {
        val list = mutableListOf<Float>()
        for (i in 0 until array.length()) {
            list.add(array.optDouble(i, 0.0).toFloat())
        }
        return if (list.isNotEmpty()) list else listOf(0f)
    }
    return listOf(obj.optDouble(key, 0.0).toFloat())
}

fun parseLabelPositions(obj: JSONObject): List<Pair<Float, Float>> {
    val xs = parseCoordinateList(obj, "label_x", "label_px_x")
    val ys = parseCoordinateList(obj, "label_y", "label_px_y")
    val count = maxOf(xs.size, ys.size)
    return List(count) { index ->
        val x = xs.getOrElse(index) { xs.lastOrNull() ?: 0f }
        val y = ys.getOrElse(index) { ys.lastOrNull() ?: 0f }
        Pair(x, y)
    }
}

fun loadStatusColors(context: Context): Map<Int, Color> {
    val defaultMap = mapOf(
        -1 to Color(0xFF495057),
        0 to Color.Transparent,
        1 to Color(0xFFC2E1FF),
        2 to Color(0xFF00BBF9),
        3 to Color(0xFFFFC300)
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
            if (!item.has("value")) continue
            val valNum = item.optInt("value")
            val colorStr = item.optString("box_background_color", "")
                .ifEmpty { item.optString("background_color", "") }
                .ifEmpty { item.optString("color", "") }
            if (colorStr.isNotEmpty()) {
                val parsedColor = if (colorStr.equals("transparent", ignoreCase = true)) {
                    Color.Transparent
                } else {
                    val hex = if (colorStr.startsWith("0x") || colorStr.startsWith("0X")) {
                        "#" + colorStr.substring(2)
                    } else if (!colorStr.startsWith("#")) {
                        "#$colorStr"
                    } else colorStr
                    try {
                        Color(android.graphics.Color.parseColor(hex))
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

fun loadStatusTextColors(context: Context): Map<Int, Color> {
    val defaultMap = mapOf(
        -1 to Color.White,
        0 to Color.Black,
        1 to Color.Black,
        2 to Color.Black,
        3 to Color.Black
    )

    val jsonString = try {
        context.assets.open("ustawienia.json").bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        return defaultMap
    }

    return try {
        val obj = JSONObject(jsonString)
        val array = obj.optJSONArray("status") ?: return defaultMap
        val map = mutableMapOf<Int, Color>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            if (!item.has("value")) continue
            val valNum = item.optInt("value")
            val colorStr = item.optString("text_color", "")
            if (colorStr.isNotEmpty()) {
                val hex = if (colorStr.startsWith("0x") || colorStr.startsWith("0X")) {
                    "#" + colorStr.substring(2)
                } else if (!colorStr.startsWith("#")) {
                    "#$colorStr"
                } else colorStr
                try {
                    map[valNum] = Color(android.graphics.Color.parseColor(hex))
                } catch (_: Exception) {}
            }
        }
        if (map.isEmpty()) defaultMap else map
    } catch (_: Exception) {
        defaultMap
    }
}

fun loadAnswerTextColors(context: Context): Map<Int, Color> {
    val jsonString = try {
        context.assets.open("ustawienia.json").bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        return emptyMap()
    }

    return try {
        val obj = JSONObject(jsonString)
        val array = obj.optJSONArray("answer") ?: return emptyMap()
        val map = mutableMapOf<Int, Color>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val statusVal = item.optInt("answer_status", -1)
            val colorStr = item.optString("text_color", "")
            if (statusVal != -1 && colorStr.isNotEmpty()) {
                val hex = if (colorStr.startsWith("0x") || colorStr.startsWith("0X")) {
                    "#" + colorStr.substring(2)
                } else if (!colorStr.startsWith("#")) {
                    "#$colorStr"
                } else colorStr
                try {
                    map[statusVal] = Color(android.graphics.Color.parseColor(hex))
                } catch (_: Exception) {}
            }
        }
        map
    } catch (_: Exception) {
        emptyMap()
    }
}

fun loadDistinctionTime(context: Context): Int {
    val jsonString = try {
        context.assets.open("ustawienia.json").bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        return 6
    }

    return try {
        val obj = JSONObject(jsonString)
        obj.optInt("distinction_time", 6)
    } catch (e: Exception) {
        6
    }
}

fun loadPassedInputTextMessage(context: Context): String {
    val jsonString = try {
        context.assets.open("ustawienia.json").bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        return "Lekcja zaliczona."
    }

    return try {
        val obj = JSONObject(jsonString)
        obj.optString("Passed_input_text_message", "Lekcja zaliczona.")
    } catch (_: Exception) {
        "Lekcja zaliczona."
    }
}

fun loadLessonNoButtonActive(context: Context): Int {
    val jsonString = try {
        context.assets.open("ustawienia.json").bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        return 2
    }

    return try {
        val obj = JSONObject(jsonString)
        obj.optInt("lesson_no_button_active", 2)
    } catch (_: Exception) {
        2
    }
}

fun loadAudioLocale(context: Context): Locale {
    val defaultCode = "PL"
    val countryCode = try {
        val jsonString = context.assets.open("ustawienia.json").bufferedReader().use { it.readText() }
        val obj = JSONObject(jsonString)
        obj.optString("iso3166-1-A-2", defaultCode).trim().uppercase()
    } catch (_: Exception) {
        defaultCode
    }.ifEmpty { defaultCode }

    return when (countryCode) {
        "PL" -> Locale.forLanguageTag("pl-PL")
        "GB", "UK" -> Locale.UK
        "US" -> Locale.US
        "DE" -> Locale.GERMANY
        "FR" -> Locale.FRANCE
        "ES" -> Locale.forLanguageTag("es-ES")
        "IT" -> Locale.ITALY
        "UA" -> Locale.forLanguageTag("uk-UA")
        "CZ" -> Locale.forLanguageTag("cs-CZ")
        "SK" -> Locale.forLanguageTag("sk-SK")
        else -> {
            Locale.getAvailableLocales().firstOrNull {
                it.country.equals(countryCode, ignoreCase = true) && it.language.isNotEmpty()
            } ?: Locale.forLanguageTag("${countryCode.lowercase()}-$countryCode")
        }
    }
}

fun loadAnswerStatusMaxAudio(context: Context): String {
    val jsonString = try {
        context.assets.open("ustawienia.json").bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        return ""
    }

    return try {
        val obj = JSONObject(jsonString)
        val audiosObj = obj.optJSONObject("audios")
        if (audiosObj != null) {
            return audiosObj.optString("answer_status_max", "").trim()
        }
        val audiosArr = obj.optJSONArray("audios")
        if (audiosArr != null) {
            for (i in 0 until audiosArr.length()) {
                val item = audiosArr.optJSONObject(i) ?: continue
                if (item.has("answer_status_max")) {
                    return item.optString("answer_status_max", "").trim()
                }
            }
        }
        obj.optString("answer_status_max", "").trim()
    } catch (_: Exception) {
        ""
    }
}

fun calculateChapterProgress(allLessons: List<LessonData>, maxStatusValue: Int = 5): Int {
    var totalQuestions = 0
    var totalStatusSum = 0
    for (lesson in allLessons) {
        for (q in lesson.questions) {
            totalQuestions++
            totalStatusSum += q.status
        }
    }
    val maxPossibleSum = totalQuestions * maxStatusValue
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
        val lessonAudio = obj.optString("audio", "").trim()
        
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
                val fullName = qObj.optString("text", "").ifEmpty { qObj.optString("full_name", "") }
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

                val labelPositions = parseLabelPositions(qObj)
                val labelWidthPx = qObj.optDouble("label_width", 0.0)
                    .let { if (it == 0.0) qObj.optDouble("label_widht", 0.0) else it }
                    .toFloat()

                val borderWidthVal = if (qObj.has("label_border_width")) {
                    qObj.optDouble("label_border_width", 1.5).toFloat()
                } else 1.5f

                val textUnderlineVal = qObj.optInt("text_underline", 0)
                val textAlignmentVal = qObj.optString("text_alignment", "center")
                val textSizeVal = qObj.optDouble("text_size", 13.0).toFloat()

                val textColorHex = qObj.optString("text_color", "")
                val textColorVal = if (textColorHex.isNotEmpty()) {
                    val hex = if (textColorHex.startsWith("0x") || textColorHex.startsWith("0X")) {
                        "#" + textColorHex.substring(2)
                    } else if (!textColorHex.startsWith("#")) {
                        "#$textColorHex"
                    } else textColorHex
                    try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { null }
                } else null

                val labelBgHex = qObj.optString("label_background_color", "")
                val labelBgColor = if (labelBgHex.isNotEmpty()) {
                    if (labelBgHex.equals("transparent", ignoreCase = true)) {
                        Color.Transparent
                    } else {
                        val bgHex = if (labelBgHex.startsWith("0x") || labelBgHex.startsWith("0X")) {
                            "#" + labelBgHex.substring(2)
                        } else if (!labelBgHex.startsWith("#")) {
                            "#$labelBgHex"
                        } else labelBgHex
                        try { Color(android.graphics.Color.parseColor(bgHex)) } catch (e: Exception) { null }
                    }
                } else null

                val labelAlphaVal = if (qObj.has("label_alpha")) qObj.optDouble("label_alpha", 1.0).toFloat() else null

                val pictureResId = if (pictureSrc.isNotEmpty()) {
                    context.resources.getIdentifier(pictureSrc, "drawable", context.packageName)
                } else 0
                val qAudio = qObj.optString("audio", "").trim()

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
                        labelPositions = labelPositions,
                        labelWidthPx = labelWidthPx,
                        borderWidth = borderWidthVal,
                        textUnderline = textUnderlineVal,
                        textColor = textColorVal,
                        labelBackgroundColor = labelBgColor,
                        labelAlpha = labelAlphaVal,
                        textAlignment = textAlignmentVal,
                        textSizeSp = textSizeVal,
                        pictureResId = if (pictureResId != 0) pictureResId else null,
                        audio = qAudio
                    )
                )
            }
        }

        val descriptionsArray = obj.optJSONArray("descriptions") ?: obj.optJSONArray("description")
        val descriptions = mutableListOf<DescriptionData>()
        if (descriptionsArray != null) {
            for (j in 0 until descriptionsArray.length()) {
                val dObj = descriptionsArray.optJSONObject(j) ?: continue
                val dId = dObj.optInt("id", j + 1)
                val dText = dObj.optString("text", "")
                val pictureSrc = dObj.optString("picture_src", "")
                val pictureAlpha = dObj.optDouble("picture_alpha", 0.3).toFloat()
                val labelPositions = parseLabelPositions(dObj)
                val labelWidthPx = dObj.optDouble("label_width", 0.0)
                    .let { if (it == 0.0) dObj.optDouble("label_widht", 0.0) else it }
                    .toFloat()

                val borderWidthVal = if (dObj.has("label_border_width")) {
                    dObj.optDouble("label_border_width", 1.5).toFloat()
                } else 1.5f

                val textUnderlineVal = dObj.optInt("text_underline", 0)
                val dTextAlignmentVal = dObj.optString("text_alignment", "center")
                val dTextSizeVal = dObj.optDouble("text_size", 13.0).toFloat()

                val dTextColorHex = dObj.optString("text_color", "")
                val dTextColorVal = if (dTextColorHex.isNotEmpty()) {
                    val hex = if (dTextColorHex.startsWith("0x") || dTextColorHex.startsWith("0X")) {
                        "#" + dTextColorHex.substring(2)
                    } else if (!dTextColorHex.startsWith("#")) {
                        "#$dTextColorHex"
                    } else dTextColorHex
                    try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { null }
                } else null

                val dLabelBgHex = dObj.optString("label_background_color", "")
                val dLabelBgColor = if (dLabelBgHex.isNotEmpty()) {
                    if (dLabelBgHex.equals("transparent", ignoreCase = true)) {
                        Color.Transparent
                    } else {
                        val bgHex = if (dLabelBgHex.startsWith("0x") || dLabelBgHex.startsWith("0X")) {
                            "#" + dLabelBgHex.substring(2)
                        } else if (!dLabelBgHex.startsWith("#")) {
                            "#$dLabelBgHex"
                        } else dLabelBgHex
                        try { Color(android.graphics.Color.parseColor(bgHex)) } catch (e: Exception) { null }
                    }
                } else null

                val dLabelAlphaVal = if (dObj.has("label_alpha")) dObj.optDouble("label_alpha", 1.0).toFloat() else null

                val pictureResId = if (pictureSrc.isNotEmpty()) {
                    context.resources.getIdentifier(pictureSrc, "drawable", context.packageName)
                } else 0
                val dAudio = dObj.optString("audio", "").trim()

                descriptions.add(
                    DescriptionData(
                        id = dId,
                        text = dText,
                        pictureSrc = pictureSrc,
                        pictureAlpha = pictureAlpha,
                        labelPositions = labelPositions,
                        labelWidthPx = labelWidthPx,
                        borderWidth = borderWidthVal,
                        textUnderline = textUnderlineVal,
                        textColor = dTextColorVal,
                        labelBackgroundColor = dLabelBgColor,
                        labelAlpha = dLabelAlphaVal,
                        textAlignment = dTextAlignmentVal,
                        textSizeSp = dTextSizeVal,
                        pictureResId = if (pictureResId != 0) pictureResId else null,
                        audio = dAudio
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
                questions = questions,
                descriptions = descriptions,
                audio = lessonAudio
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
    onHomeClicked: () -> Unit,
    onBackToChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    val imeInsetsVisible = WindowInsets.isImeVisible
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val navBarBottomPx = WindowInsets.navigationBars.getBottom(density)

    var isKeyboardOpenByRect by remember { mutableStateOf(false) }
    var rectKeypadHeightPx by remember { mutableIntStateOf(0) }
    var isInputFocused by remember { mutableStateOf(false) }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val rootHeight = view.rootView.height
            val keypadHeight = (rootHeight - rect.bottom).coerceAtLeast(0)
            val isOpen = rootHeight > 0 && keypadHeight > rootHeight * 0.15
            isKeyboardOpenByRect = isOpen
            rectKeypadHeightPx = if (isOpen) keypadHeight else 0
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    val systemKeyboardVisible = imeInsetsVisible || isKeyboardOpenByRect
    var wasSystemKeyboardVisible by remember { mutableStateOf(false) }

    LaunchedEffect(systemKeyboardVisible) {
        if (systemKeyboardVisible) {
            wasSystemKeyboardVisible = true
        } else if (wasSystemKeyboardVisible) {
            wasSystemKeyboardVisible = false
            isInputFocused = false
            focusManager.clearFocus()
        }
    }

    BackHandler(enabled = isInputFocused && !systemKeyboardVisible) {
        isInputFocused = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val isImeVisible = systemKeyboardVisible || isInputFocused

    val fallbackImeBottomPadding = if (imeBottomPx == 0 && rectKeypadHeightPx > 0) {
        with(density) { (rectKeypadHeightPx - navBarBottomPx).coerceAtLeast(0).toDp() }
    } else {
        0.dp
    }

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
    val statusTextColors = remember { loadStatusTextColors(context) }
    val answerTextColors = remember { loadAnswerTextColors(context) }
    val maxStatusValue = remember(answerTextColors) {
        answerTextColors.keys.maxOrNull() ?: 5
    }
    val distinctionTimeSeconds = remember { loadDistinctionTime(context) }
    val passedInputTextMessage = remember { loadPassedInputTextMessage(context) }
    val lessonNoButtonActive = remember { loadLessonNoButtonActive(context) }
    val audioLocale = remember { loadAudioLocale(context) }
    val answerStatusMaxAudio = remember { loadAnswerStatusMaxAudio(context) }

    var isAudioMuted by rememberSaveable { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var pendingSpeech by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context, audioLocale) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = ttsInstance
                if (engine != null) {
                    val langResult = engine.setLanguage(audioLocale)
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine.setLanguage(Locale.forLanguageTag(audioLocale.language))
                    }
                    isTtsReady = true
                    val toSpeak = pendingSpeech
                    if (!isAudioMuted && !toSpeak.isNullOrBlank()) {
                        pendingSpeech = null
                        engine.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, "fennec_tts")
                    }
                }
            }
        }
        tts = ttsInstance
        onDispose {
            pendingSpeech = null
            isTtsReady = false
            ttsInstance.stop()
            ttsInstance.shutdown()
            tts = null
        }
    }

    fun speakText(text: String) {
        val cleanText = text.trim()
        if (isAudioMuted || cleanText.isEmpty()) return
        val engine = tts
        if (isTtsReady && engine != null) {
            pendingSpeech = null
            engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "fennec_tts")
        } else {
            pendingSpeech = cleanText
        }
    }

    fun stopSpeech() {
        pendingSpeech = null
        tts?.stop()
    }

    val rawLessons = remember(databaseFilename) {
        loadLessons(context, databaseFilename)
    }

    val allLessons = remember(rawLessons, allowedLessonIds) {
        if (allowedLessonIds.isNotEmpty()) {
            val lessonsById = rawLessons.associateBy { it.id }
            allowedLessonIds.mapNotNull { id -> lessonsById[id] }
        } else {
            rawLessons
        }
    }

    var currentLessonIndex by rememberSaveable(selectedChapter?.id, selectedLevel) {
        mutableIntStateOf(0)
    }
    val currentLesson = allLessons.getOrNull(currentLessonIndex)

    var lastSpokenLessonKey by rememberSaveable { mutableStateOf<String?>(null) }
    val currentLessonKey = if (currentLesson != null) "${databaseFilename}_${currentLessonIndex}_${currentLesson.id}" else null

    LaunchedEffect(currentLessonKey) {
        if (currentLessonKey != null && currentLessonKey != lastSpokenLessonKey) {
            lastSpokenLessonKey = currentLessonKey
            stopSpeech()
            val lessonAudio = currentLesson?.audio?.trim().orEmpty()
            if (!isAudioMuted && lessonAudio.isNotEmpty()) {
                speakText(lessonAudio)
            }
        }
    }

    val questions = currentLesson?.questions ?: emptyList()

    // Auto-detect mastered questions (status >= maxStatusValue) so they are pre-revealed without pulsing
    val knownItemIds = remember(currentLesson, maxStatusValue) {
        questions.filter { it.status >= maxStatusValue }.map { it.id }.toSet()
    }

    var guessedItems by remember(currentLesson) { mutableStateOf(knownItemIds) }
    var lastGuessedItem by remember(currentLesson) { mutableStateOf<Int?>(null) }
    var pulseTrigger by remember(currentLesson) { mutableIntStateOf(0) }
    var inputText by rememberSaveable(currentLesson?.id) { mutableStateOf("") }
    var hintLevel by rememberSaveable(currentLesson?.id) { mutableIntStateOf(0) } // 0: empty, 1: hint1, 2: hint2

    LaunchedEffect(lastGuessedItem, pulseTrigger) {
        if (lastGuessedItem != null && distinctionTimeSeconds > 0) {
            delay(distinctionTimeSeconds * 1000L)
            lastGuessedItem = null
        }
    }

    val chapterProgress = remember(allLessons, guessedItems, maxStatusValue) {
        calculateChapterProgress(allLessons, maxStatusValue)
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
            .padding(bottom = fallbackImeBottomPadding)
            .verticalScroll(scrollState)
            .padding(
                start = if (isImeVisible) 8.dp else 16.dp,
                end = if (isImeVisible) 8.dp else 16.dp,
                top = if (isImeVisible) 2.dp else 12.dp,
                bottom = if (isImeVisible) 2.dp else 12.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isImeVisible) 4.dp else 12.dp)
    ) {
        val colorMinus1 = statusColors[-1] ?: Color(0xFF495057)
        val color0 = statusColors[0] ?: Color.Transparent
        val color1 = statusColors[1] ?: Color(0xFFC2E1FF)
        val color2 = statusColors[2] ?: Color(0xFF00BBF9)
        val color3 = statusColors[3] ?: Color(0xFFFFC300)
        val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

        fun getLessonStatusLevel(lesson: LessonData): Int {
            if (lesson.questions.isEmpty()) return -1
            val maxPossibleSum = lesson.questions.size * maxStatusValue
            val statusSum = lesson.questions.sumOf { it.status }
            return when {
                maxPossibleSum == 0 || statusSum <= 0 -> 0
                statusSum >= maxPossibleSum -> 3
                statusSum * 2 <= maxPossibleSum -> 1
                else -> 2
            }
        }

        fun getLessonStatusColor(lesson: LessonData): Color {
            return when (getLessonStatusLevel(lesson)) {
                -1 -> colorMinus1
                0 -> color0
                1 -> color1
                2 -> color2
                else -> color3
            }
        }

        fun getLessonStatusTextColor(lesson: LessonData): Color {
            val level = getLessonStatusLevel(lesson)
            return statusTextColors[level] ?: if (level == -1) Color.White else Color.Black
        }

        if (isImeVisible) {
            // Wiersz z płaskimi prostokątami symbolizującymi wszystkie lekcje
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in allLessons.indices) {
                    val isCurrent = i == currentLessonIndex
                    val lessonObj = allLessons[i]
                    val rectBg = getLessonStatusColor(lessonObj)

                    val borderMod = if (isCurrent) {
                        Modifier.border(2.5.dp, Color.Black, RoundedCornerShape(3.dp))
                    } else if (rectBg == Color.Transparent) {
                        Modifier.border(1.dp, outlineColor, RoundedCornerShape(3.dp))
                    } else {
                        Modifier
                    }

                    val isTileClickable = when (lessonNoButtonActive) {
                        0 -> false
                        1 -> i > currentLessonIndex
                        else -> true
                    }
                    val clickMod = if (isTileClickable) {
                        Modifier.clickable { currentLessonIndex = i }
                    } else {
                        Modifier
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .background(rectBg, RoundedCornerShape(3.dp))
                            .then(borderMod)
                            .then(clickMod)
                    )
                }
            }
        } else {
            // Header Card (Subject, Class, Chapter, Favorite Star, Progress & Lesson Tiles)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Wiersz 1: Przedmiot Klasa (lewo) | Rozdział X ★ (środek) | Progres: Y% (prawo)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var isStarred by remember(selectedChapter?.id, selectedChapter?.isFavorite) {
                            mutableStateOf(selectedChapter?.isFavorite == 1)
                        }
                        val chapterNumStr = selectedChapter?.chapterNum?.ifEmpty { selectedChapter.id.toString() } ?: ""

                        Text(
                            text = "${AppGlobalState.selectedSubject} ${AppGlobalState.selectedClass}",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Rozdział $chapterNumStr ",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Ulubione",
                                tint = if (isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        isStarred = !isStarred
                                        val newFav = if (isStarred) 1 else 0
                                        selectedChapter?.isFavorite = newFav
                                        if (selectedChapter != null) {
                                            saveChapterFavorite(
                                                context = context,
                                                textbookTitle = AppGlobalState.selectedTextbook?.title ?: "",
                                                chapterId = selectedChapter.id,
                                                isFavorite = newFav
                                            )
                                        }
                                    }
                            )
                        }

                        Text(
                            text = "Progres: $chapterProgress%",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Dynamic Lesson Tiles L01, L02... (Justified rows, last partial row aligned left to columns)
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val minTileSlotWidth = 48.dp
                        val columnsPerRow = maxOf(1, (maxWidth / minTileSlotWidth).toInt())
                        val rows = allLessons.indices.chunked(columnsPerRow)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (rowIndices in rows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (col in 0 until columnsPerRow) {
                                        val i = rowIndices.getOrNull(col)
                                        if (i != null) {
                                            val label = "L${(i + 1).toString().padStart(2, '0')}"
                                            val isCurrent = i == currentLessonIndex
                                            val lessonObj = allLessons[i]
                                            val isMastered = lessonObj.questions.isNotEmpty() && lessonObj.questions.all { it.status >= maxStatusValue }

                                            val tileBg = getLessonStatusColor(lessonObj)

                                            val tileBorder = if (isCurrent) {
                                                BorderStroke(2.5.dp, Color.Black)
                                            } else if (tileBg == Color.Transparent) {
                                                BorderStroke(1.dp, outlineColor)
                                            } else null

                                            val tileTextColor = getLessonStatusTextColor(lessonObj)

                                            val borderMod = if (tileBorder != null) Modifier.border(tileBorder, RoundedCornerShape(8.dp)) else Modifier
                                            val isTileClickable = when (lessonNoButtonActive) {
                                                0 -> false
                                                1 -> i > currentLessonIndex
                                                else -> true
                                            }
                                            val clickMod = if (isTileClickable) {
                                                Modifier.clickable { currentLessonIndex = i }
                                            } else {
                                                Modifier
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(tileBg, RoundedCornerShape(8.dp))
                                                    .then(borderMod)
                                                    .then(clickMod)
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = tileTextColor,
                                                    fontWeight = if (isCurrent || isMastered) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
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
                .aspectRatio(canvasWidthRatio / canvasHeightRatio)
                .pointerInput(currentLessonIndex, allLessons.size) {
                    var totalDragX = 0f
                    val swipeThresholdPx = 48.dp.toPx()
                    detectHorizontalDragGestures(
                        onDragStart = { totalDragX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount
                        },
                        onDragEnd = {
                            // Przesunięcie palcem od prawej do lewej (totalDragX < 0) -> następna lekcja
                            if (totalDragX < -swipeThresholdPx && currentLessonIndex < allLessons.size - 1) {
                                currentLessonIndex++
                            }
                        }
                    )
                },
            colors = CardDefaults.cardColors(containerColor = bgColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val containerWidth = maxWidth
                val containerHeight = maxHeight
                val canvasScale = containerWidth / 376.dp
                val scaledCorner = 8.dp * canvasScale
                val scaledPadH = 8.dp * canvasScale
                val scaledPadV = 4.dp * canvasScale

                // Static / Decorative Description Layer
                currentLesson?.descriptions?.forEach { desc ->
                    if (desc.pictureResId != null) {
                        Image(
                            painter = painterResource(id = desc.pictureResId),
                            contentDescription = desc.text.ifEmpty { null },
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(desc.pictureAlpha),
                            alignment = Alignment.TopStart,
                            contentScale = ContentScale.FillBounds
                        )
                    }

                    if (desc.text.isNotEmpty()) {
                        val labelWidthDp = if (desc.labelWidthPx > 0f) containerWidth * (desc.labelWidthPx / canvasWidthRatio) else null
                        val borderWidthDp = desc.borderWidth.dp * canvasScale
                        val descFontSize = with(density) { (desc.textSizeSp.dp * canvasScale).toSp() }
                        val descLineHeight = with(density) { ((desc.textSizeSp * 1.15f).dp * canvasScale).toSp() }

                        val isTransparentBg = desc.labelBackgroundColor == Color.Transparent || desc.labelAlpha == 0f
                        val boxBgColor = if (isTransparentBg) {
                            Color.Transparent
                        } else {
                            val baseColor = desc.labelBackgroundColor ?: Color.White
                            val alphaVal = desc.labelAlpha ?: 0.85f
                            baseColor.copy(alpha = alphaVal)
                        }

                        val borderAlpha = desc.labelAlpha ?: 1.0f
                        val boxWidthMod = if (labelWidthDp != null) Modifier.width(labelWidthDp) else Modifier
                        val borderMod = if (borderWidthDp > 0.dp && borderAlpha > 0f && !isTransparentBg) {
                            Modifier.border(borderWidthDp, Color.Black.copy(alpha = borderAlpha), RoundedCornerShape(scaledCorner))
                        } else Modifier
                        val descClickMod = if (desc.audio.isNotEmpty()) {
                            Modifier.clickable {
                                if (!isAudioMuted) {
                                    speakText(desc.audio)
                                }
                            }
                        } else Modifier

                        desc.labelPositions.forEach { (pxX, pxY) ->
                            val labelOffsetX = containerWidth * (pxX / canvasWidthRatio)
                            val labelOffsetY = containerHeight * (pxY / canvasHeightRatio)

                            Box(
                                modifier = Modifier
                                    .offset(labelOffsetX, labelOffsetY)
                                    .then(boxWidthMod)
                                    .background(boxBgColor, RoundedCornerShape(scaledCorner))
                                    .then(borderMod)
                                    .then(descClickMod)
                                    .padding(horizontal = scaledPadH, vertical = scaledPadV),
                                contentAlignment = parseBoxAlignment(desc.textAlignment)
                            ) {
                                Text(
                                    text = desc.text,
                                    color = desc.textColor ?: Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = descFontSize,
                                    lineHeight = descLineHeight,
                                    textAlign = parseTextAlign(desc.textAlignment),
                                    textDecoration = if (desc.textUnderline == 1) TextDecoration.Underline else TextDecoration.None
                                )
                            }
                        }
                    }
                }

                questions.forEach { question ->
                    val isGuessed = guessedItems.contains(question.id)
                    val isLastGuessed = lastGuessedItem == question.id

                    val currentScale = if (isLastGuessed) scale else 1.0f
                    val baseImageAlpha = if (question.status >= maxStatusValue) 1.0f else question.pictureAlpha
                    val imageAlpha = if (isLastGuessed) pulseAlpha else if (isGuessed) 1.0f else baseImageAlpha
                    val labelBoxAlpha = if (isLastGuessed) pulseAlpha else 1.0f

                    if (question.pictureResId != null) {
                        Image(
                            painter = painterResource(id = question.pictureResId),
                            contentDescription = question.fullName,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(currentScale)
                                .alpha(imageAlpha),
                            alignment = Alignment.TopStart,
                            contentScale = ContentScale.FillBounds
                        )
                    }

                    val labelWidthDp = if (question.labelWidthPx > 0f) containerWidth * (question.labelWidthPx / canvasWidthRatio) else null
                    val borderWidthDp = question.borderWidth.dp * canvasScale
                    val qFontSize = with(density) { (question.textSizeSp.dp * canvasScale).toSp() }
                    val qLineHeight = with(density) { ((question.textSizeSp * 1.15f).dp * canvasScale).toSp() }

                    val resolvedFullText = if (question.fullName.isNotEmpty()) question.fullName else question.answer
                    val labelText = if (isGuessed) {
                        resolvedFullText
                    } else {
                        when (hintLevel) {
                            0 -> question.empty
                            1 -> question.hint1
                            else -> question.hint2
                        }
                    }

                    val isTransparentBg = question.labelBackgroundColor == Color.Transparent || question.labelAlpha == 0f
                    val boxBgColor = if (isTransparentBg) {
                        Color.Transparent
                    } else {
                        val baseColor = question.labelBackgroundColor ?: Color.White
                        val alphaVal = question.labelAlpha ?: 0.85f
                        baseColor.copy(alpha = alphaVal)
                    }

                    val baseBorderColor = if (isGuessed) Color(0xFF001F3F) else Color.Black
                    val borderAlpha = question.labelAlpha ?: 1.0f

                    val boxWidthMod = if (labelWidthDp != null) Modifier.width(labelWidthDp) else Modifier
                    val borderMod = if (borderWidthDp > 0.dp && borderAlpha > 0f && !isTransparentBg) {
                        Modifier.border(borderWidthDp, baseBorderColor.copy(alpha = borderAlpha), RoundedCornerShape(scaledCorner))
                    } else Modifier

                    val questionTextColor = if (question.status >= maxStatusValue && question.textColor != null) {
                        question.textColor
                    } else {
                        answerTextColors[question.status] ?: Color.Black
                    }

                    question.labelPositions.forEach { (pxX, pxY) ->
                        val labelOffsetX = containerWidth * (pxX / canvasWidthRatio)
                        val labelOffsetY = containerHeight * (pxY / canvasHeightRatio)

                        Box(
                            modifier = Modifier
                                .offset(labelOffsetX, labelOffsetY)
                                .scale(currentScale)
                                .alpha(labelBoxAlpha)
                                .then(boxWidthMod)
                                .background(boxBgColor, RoundedCornerShape(scaledCorner))
                                .then(borderMod)
                                .clickable {
                                    lastGuessedItem = question.id
                                    pulseTrigger++
                                    if (!isAudioMuted) {
                                        val audioToSpeak = if (question.status >= maxStatusValue && answerStatusMaxAudio.isNotEmpty()) {
                                            answerStatusMaxAudio
                                        } else {
                                            question.audio
                                        }
                                        if (audioToSpeak.isNotEmpty()) {
                                            speakText(audioToSpeak)
                                        }
                                    }
                                }
                                .padding(horizontal = scaledPadH, vertical = scaledPadV),
                            contentAlignment = parseBoxAlignment(question.textAlignment)
                        ) {
                            if (labelWidthDp == null && question.textAlignment.equals("right", ignoreCase = true)) {
                                listOf(question.empty, question.hint1, question.hint2, resolvedFullText)
                                    .distinct()
                                    .forEach { refText ->
                                        Text(
                                            text = refText,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = qFontSize,
                                            lineHeight = qLineHeight,
                                            modifier = Modifier.height(0.dp).alpha(0f)
                                        )
                                    }
                            }
                            Text(
                                text = labelText,
                                color = questionTextColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = qFontSize,
                                lineHeight = qLineHeight,
                                textAlign = parseTextAlign(question.textAlignment),
                                textDecoration = if (question.textUnderline == 1) TextDecoration.Underline else TextDecoration.None
                            )
                        }
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

        // Hints & Navigation Row (visible ONLY when keyboard is open)
        if (isImeVisible) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Hide Keyboard Button
                IconButton(
                    onClick = {
                        isInputFocused = false
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_keyboard_off_24),
                        contentDescription = "Schowaj klawiaturę",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 2. Back to Chapter Button
                Button(
                    onClick = onBackToChapter,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_menu_book_24),
                        contentDescription = "Rozdział"
                    )
                }

                // 3. Speaker / Mute Button
                Button(
                    onClick = {
                        isAudioMuted = !isAudioMuted
                        if (isAudioMuted) {
                            stopSpeech()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF546E7A),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isAudioMuted) R.drawable.mute_icon else R.drawable.speaker_icon
                        ),
                        contentDescription = if (isAudioMuted) "Włącz dźwięk" else "Wycisz dźwięk"
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 4. Hint Button with Lightbulbs inside
                Button(
                    onClick = {
                        if (hintLevel < 2) hintLevel++
                    },
                    enabled = hintLevel < 2,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hintLevel < 2) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (hintLevel < 2) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (hintLevel) {
                            0 -> "Podpowiedź"
                            1 -> "Odkryj więcej"
                            else -> "Brak podpowiedzi"
                        },
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = "Podpowiedź 1",
                        tint = if (hintLevel >= 1) Color(0xFFFFC107) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = "Podpowiedź 2",
                        tint = if (hintLevel >= 2) Color(0xFFFFC107) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Answer Input Field
        val isLessonPassed = questions.isNotEmpty() && questions.all { it.status >= maxStatusValue }

        LaunchedEffect(isLessonPassed) {
            if (isLessonPassed && isInputFocused) {
                isInputFocused = false
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        }

        if (currentLesson != null && questions.isNotEmpty()) {
            OutlinedTextField(
                value = if (isLessonPassed) passedInputTextMessage else inputText,
                onValueChange = { newValue ->
                    if (!isLessonPassed) {
                        inputText = newValue
                        val match = questions.find { q ->
                            !guessedItems.contains(q.id) && q.acceptedAnswers.any { word ->
                                word.equals(newValue.trim(), ignoreCase = true)
                            }
                        }
                        if (match != null) {
                            guessedItems = guessedItems + match.id
                            lastGuessedItem = match.id
                            pulseTrigger++

                            val oldStatus = match.status
                            val proposedStatus = when (hintLevel) {
                                2 -> 1
                                1 -> 2
                                else -> if (oldStatus < 3) 3 else (oldStatus + 1).coerceAtMost(maxStatusValue)
                            }
                            val newStatus = maxOf(oldStatus, proposedStatus).coerceAtMost(maxStatusValue)
                            match.status = newStatus

                            saveQuestionStatus(
                                context = context,
                                databaseFilename = databaseFilename,
                                lessonId = currentLesson.id,
                                questionId = match.id,
                                newStatus = newStatus
                            )

                            inputText = ""
                        }
                    }
                },
                enabled = !isLessonPassed,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!isLessonPassed) {
                            isInputFocused = focusState.isFocused
                        }
                    },
                placeholder = {
                    Text(if (isLessonPassed) passedInputTextMessage else currentLesson.task)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false
                ),
                visualTransformation = VisualTransformation.None,
                singleLine = true
            )
        }

        if (!isImeVisible) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onHomeClicked
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_home_24),
                        contentDescription = "Ekran główny"
                    )
                }
                Button(
                    onClick = onBackToChapter,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_menu_book_24),
                        contentDescription = "Rozdział"
                    )
                }
                Button(
                    onClick = {
                        isAudioMuted = !isAudioMuted
                        if (isAudioMuted) {
                            stopSpeech()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF546E7A),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isAudioMuted) R.drawable.mute_icon else R.drawable.speaker_icon
                        ),
                        contentDescription = if (isAudioMuted) "Włącz dźwięk" else "Wycisz dźwięk"
                    )
                }
            }
        }
    }
}

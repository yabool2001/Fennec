package school.fennec

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import java.io.File
import school.fennec.ui.theme.FennecTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FennecTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FennecApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

object AppGlobalState {
    var selectedClass: Int by mutableIntStateOf(6)
    var selectedSubject: String by mutableStateOf("Biologia")
    var selectedTextbook: Textbook? by mutableStateOf(null)
    var selectedChapter: Chapter? by mutableStateOf(null)
    var selectedLevel: String by mutableStateOf("Fundamenty")
    var selectedMode: String by mutableStateOf("Nauka")
}

enum class AppState {
    MAIN_SETUP, CHAPTERS, LEARNING_OPTIONS, LABELLING_GAME
}

@Composable
fun FennecApp(modifier: Modifier = Modifier) {
    var currentState by remember { mutableStateOf(AppState.MAIN_SETUP) }
    
    // Obsługa sprzętowego/systemowego powrotu
    BackHandler(enabled = currentState != AppState.MAIN_SETUP) {
        currentState = when (currentState) {
            AppState.CHAPTERS -> AppState.MAIN_SETUP
            AppState.LEARNING_OPTIONS -> AppState.CHAPTERS
            AppState.LABELLING_GAME -> AppState.LEARNING_OPTIONS
            AppState.MAIN_SETUP -> AppState.MAIN_SETUP // Nigdy tu nie wejdzie
        }
    }

    when (currentState) {
        AppState.MAIN_SETUP -> {
            MainSetupScreen(
                onNextClicked = { currentState = AppState.CHAPTERS },
                modifier = modifier
            )
        }
        AppState.CHAPTERS -> {
            ChapterSelectionScreen(
                onChapterSelected = { currentState = AppState.LEARNING_OPTIONS },
                onBackClicked = { currentState = AppState.MAIN_SETUP },
                modifier = modifier
            )
        }
        AppState.LEARNING_OPTIONS -> {
            LearningOptionsScreen(
                onBackClicked = { currentState = AppState.CHAPTERS },
                onStartGameClicked = { currentState = AppState.LABELLING_GAME },
                modifier = modifier
            )
        }
        AppState.LABELLING_GAME -> {
            val context = LocalContext.current
            LabellingGameScreen(
                onBackToChapter = { currentState = AppState.CHAPTERS },
                onEndLearning = {
                    (context as? Activity)?.finish()
                },
                modifier = modifier
            )
        }
    }
}

data class Chapter(
    val id: Int,
    val title: String,
    val resFilename: String?,
    val imageResId: Int?,
    val fundamentyDatabase: String = "bio_lekcje.json",
    val fundamentyLessonIds: List<Int> = emptyList(),
    val doskonalenieDatabase: String = "bio_lekcje.json",
    val doskonalenieLessonIds: List<Int> = emptyList()
)

data class Textbook(
    val publisher: String,
    val title: String,
    val classNum: Int,
    val subject: String,
    val coverResFilename: String,
    val coverResId: Int?,
    val chapters: List<Chapter>
)

fun loadTextbooks(context: Context, classNum: Int, subject: String): List<Textbook> {
    val jsonString = try {
        context.assets.open("podreczniki.json").bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        return emptyList()
    }

    val jsonArray = JSONArray(jsonString)
    val result = mutableListOf<Textbook>()

    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.optJSONObject(i) ?: continue
        val itemKlasa = obj.optInt("klasa", -1)
        val itemPrzedmiot = obj.optString("przedmiot", "")

        if (itemKlasa == classNum && itemPrzedmiot.equals(subject, ignoreCase = true)) {
            val coverFilename = obj.optString("okladka_res_filename", "")
                .ifEmpty { obj.optString("okladka_res", "") }

            val resId = if (coverFilename.isNotEmpty()) {
                context.resources.getIdentifier(coverFilename, "drawable", context.packageName)
            } else 0

            val chaptersList = mutableListOf<Chapter>()
            val chaptersArray = obj.optJSONArray("rozdzial") ?: obj.optJSONArray("rozdzialy")
            if (chaptersArray != null) {
                for (j in 0 until chaptersArray.length()) {
                    val chObj = chaptersArray.optJSONObject(j) ?: continue
                    val chId = chObj.optInt("id", j + 1)
                    val chTitle = chObj.optString("tytul", "")
                    val chResFilename = chObj.optString("res_filename", "")
                        .ifEmpty { chObj.optString("grafika_res", "") }

                    val chResId = if (chResFilename.isNotEmpty()) {
                        context.resources.getIdentifier(chResFilename, "drawable", context.packageName)
                    } else 0

                    val fundamentyObj = chObj.optJSONObject("fundamenty")
                    val doskonalenieObj = chObj.optJSONObject("doskonalenie")

                    var fundDb = "bio_lekcje.json"
                    val fundLessonIds = mutableListOf<Int>()
                    if (fundamentyObj != null) {
                        val db = fundamentyObj.optString("database", "").ifEmpty { fundamentyObj.optString("databse", "") }
                        if (db.isNotEmpty()) fundDb = db
                        val arr = fundamentyObj.optJSONArray("lekcje")
                        if (arr != null) {
                            for (k in 0 until arr.length()) {
                                fundLessonIds.add(arr.optInt(k))
                            }
                        }
                    }

                    var doskDb = "bio_lekcje.json"
                    val doskLessonIds = mutableListOf<Int>()
                    if (doskonalenieObj != null) {
                        val db = doskonalenieObj.optString("database", "").ifEmpty { doskonalenieObj.optString("databse", "") }
                        if (db.isNotEmpty()) doskDb = db
                        val arr = doskonalenieObj.optJSONArray("lekcje")
                        if (arr != null) {
                            for (k in 0 until arr.length()) {
                                doskLessonIds.add(arr.optInt(k))
                            }
                        }
                    }

                    chaptersList.add(
                        Chapter(
                            id = chId,
                            title = chTitle,
                            resFilename = chResFilename.ifEmpty { null },
                            imageResId = if (chResId != 0) chResId else null,
                            fundamentyDatabase = fundDb,
                            fundamentyLessonIds = fundLessonIds,
                            doskonalenieDatabase = doskDb,
                            doskonalenieLessonIds = doskLessonIds
                        )
                    )
                }
            }

            result.add(
                Textbook(
                    publisher = obj.optString("wydawnictwo", ""),
                    title = obj.optString("tytul", ""),
                    classNum = itemKlasa,
                    subject = itemPrzedmiot,
                    coverResFilename = coverFilename,
                    coverResId = if (resId != 0) resId else null,
                    chapters = chaptersList
                )
            )
        }
    }

    return result
}

@Composable
fun MainSetupScreen(
    onNextClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textbooks = remember(AppGlobalState.selectedClass, AppGlobalState.selectedSubject) {
        loadTextbooks(context, AppGlobalState.selectedClass, AppGlobalState.selectedSubject)
    }

    LaunchedEffect(textbooks) {
        if (AppGlobalState.selectedTextbook !in textbooks) {
            AppGlobalState.selectedTextbook = textbooks.firstOrNull()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Fenek",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
        )

        // 1. Wybór klasy
        Text(
            text = "Wybierz klasę",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(6 to "Klasa 6", 7 to "Klasa 7", 8 to "Klasa 8").forEach { (classNum, className) ->
                SelectionCard(
                    title = className,
                    isSelected = AppGlobalState.selectedClass == classNum,
                    onClick = { AppGlobalState.selectedClass = classNum },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Wybór przedmiotu
        Text(
            text = "Wybierz przedmiot",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Biologia", "Geografia", "Historia").forEach { subject ->
                SelectionCard(
                    title = subject,
                    isSelected = AppGlobalState.selectedSubject == subject,
                    onClick = { AppGlobalState.selectedSubject = subject },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Wybór podręcznika
        Text(
            text = "Wybierz podręcznik",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(textbooks) { textbook ->
                val isSelected = AppGlobalState.selectedTextbook == textbook
                Card(
                    modifier = Modifier
                        .height(if (isSelected) 215.dp else 185.dp)
                        .scale(if (isSelected) 1.08f else 0.95f)
                        .clickable { AppGlobalState.selectedTextbook = textbook },
                    shape = RectangleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 12.dp else 2.dp),
                    // Zaznaczony podręcznik otrzymuje wyraźną, kolorową ramkę!
                    border = if (isSelected) BorderStroke(4.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    if (textbook.coverResId != null) {
                        Image(
                            painter = painterResource(id = textbook.coverResId),
                            contentDescription = textbook.title,
                            modifier = Modifier.fillMaxHeight(),
                            contentScale = ContentScale.FillHeight
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .fillMaxHeight()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = textbook.title,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Przycisk Dalej (zamiast powrotu, bo stąd nie ma gdzie wracać)
        Button(
            onClick = onNextClicked,
            modifier = Modifier
                .padding(top = 48.dp)
                .width(200.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50), 
                contentColor = Color.White
            )
        ) {
            Text(text = "Dalej")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Dalej"
            )
        }

        // Przycisk Ulubione
        Button(
            onClick = { /* TODO: Przejście do Ulubionych */ },
            modifier = Modifier
                .padding(top = 16.dp)
                .width(200.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = "Ulubione",
                tint = Color(0xFFFFC107) // Ikona gwiazdki w kolorze złoto-żółtym (Amber)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Ulubione")
        }
        
        // Przycisk Poprzednia lekcja
        Button(
            onClick = { /* TODO: Przejście do poprzedniej lekcji */ },
            modifier = Modifier
                .padding(top = 16.dp, bottom = 48.dp)
                .width(200.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = "Poprzednia lekcja"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Poprzednia lekcja")
        }
    }
}

@Composable
fun ChapterSelectionScreen(
    onChapterSelected: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTextbook = AppGlobalState.selectedTextbook
    val chapters = selectedTextbook?.chapters ?: emptyList()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wybierz rozdział",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(top = 48.dp, bottom = 32.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().weight(1f) // Pozwalamy siatce zająć dostępne miejsce nad przyciskiem
        ) {
            items(chapters) { chapter ->
                Card(
                    modifier = Modifier
                        .height(140.dp)
                        .clickable { 
                            AppGlobalState.selectedChapter = chapter
                            onChapterSelected() 
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (chapter.imageResId != null) {
                            Image(
                                painter = painterResource(id = chapter.imageResId),
                                contentDescription = chapter.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Text(
                            text = chapter.title,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        Button(
            onClick = onBackClicked,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 32.dp)
                .width(200.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Wróć"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Wróć")
        }
    }
}

fun resetChapterProgress(context: Context, databaseFilename: String) {
    val filename = if (databaseFilename.isNotEmpty()) databaseFilename else "bio_lekcje.json"
    val file = File(context.filesDir, filename)

    val assetsJsonString = try {
        context.assets.open(filename).bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        return
    }

    try {
        val jsonArray = JSONArray(assetsJsonString)
        for (i in 0 until jsonArray.length()) {
            val lessonObj = jsonArray.optJSONObject(i) ?: continue
            val qArray = lessonObj.optJSONArray("questions") ?: lessonObj.optJSONArray("answers")
            if (qArray != null) {
                for (j in 0 until qArray.length()) {
                    val qObj = qArray.optJSONObject(j) ?: continue
                    qObj.put("status", 0)
                }
            }
        }
        file.writeText(jsonArray.toString(2))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun LearningOptionsScreen(
    onBackClicked: () -> Unit,
    onStartGameClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wybierz poziom trudności",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 48.dp, bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SelectionCard(
                title = "Fundamenty",
                subtitle = "opanuj podstawy",
                isSelected = AppGlobalState.selectedLevel == "Fundamenty",
                onClick = { AppGlobalState.selectedLevel = "Fundamenty" },
                modifier = Modifier.weight(1f)
            )
            SelectionCard(
                title = "Doskonalenie",
                subtitle = "rozwiń swoją wiedzę",
                isSelected = AppGlobalState.selectedLevel == "Doskonalenie",
                onClick = { AppGlobalState.selectedLevel = "Doskonalenie" },
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "i co robimy",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SelectionCard(
                title = "Nauka",
                isSelected = AppGlobalState.selectedMode == "Nauka",
                onClick = { AppGlobalState.selectedMode = "Nauka" },
                modifier = Modifier.weight(1f)
            )
            SelectionCard(
                title = "Test",
                isSelected = AppGlobalState.selectedMode == "Test",
                onClick = { AppGlobalState.selectedMode = "Test" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Row with Wróć on left and Zaczynamy on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onBackClicked,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Wróć"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Wróć")
            }

            Button(
                onClick = onStartGameClicked,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                )
            ) {
                Text(text = "Zaczynamy")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Zaczynamy"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Reset Progres Button
        Button(
            onClick = {
                val chapter = AppGlobalState.selectedChapter
                val isFundamenty = AppGlobalState.selectedLevel.equals("Fundamenty", ignoreCase = true)
                val dbFilename = if (isFundamenty) {
                    chapter?.fundamentyDatabase ?: "bio_lekcje.json"
                } else {
                    chapter?.doskonalenieDatabase ?: "bio_lekcje.json"
                }
                resetChapterProgress(context, dbFilename)
            },
            modifier = Modifier
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Resetuj progres"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Resetuj progres")
        }
    }
}

@Composable
fun SelectionCard(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FennecAppPreview() {
    FennecTheme {
        FennecApp()
    }
}

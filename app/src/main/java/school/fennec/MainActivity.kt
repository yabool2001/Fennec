package school.fennec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

enum class AppState {
    SUBJECTS, CLASSES, TEXTBOOKS, CHAPTERS, LEARNING_OPTIONS
}

@Composable
fun FennecApp(modifier: Modifier = Modifier) {
    var currentState by remember { mutableStateOf(AppState.SUBJECTS) }
    var selectedSubject by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("") }
    
    // Obsługa sprzętowego/systemowego powrotu
    BackHandler(enabled = currentState != AppState.SUBJECTS) {
        currentState = when (currentState) {
            AppState.CLASSES -> AppState.SUBJECTS
            AppState.TEXTBOOKS -> AppState.CLASSES
            AppState.CHAPTERS -> AppState.TEXTBOOKS
            AppState.LEARNING_OPTIONS -> AppState.CHAPTERS
            AppState.SUBJECTS -> AppState.SUBJECTS // Nigdy tu nie wejdzie
        }
    }

    when (currentState) {
        AppState.SUBJECTS -> {
            MenuScreen(
                title = "Fenek",
                options = listOf("Biologia", "Geografia", "Historia"),
                onOptionSelected = { subject ->
                    selectedSubject = subject
                    currentState = AppState.CLASSES
                },
                modifier = modifier
            )
        }
        AppState.CLASSES -> {
            MenuScreen(
                title = selectedSubject,
                options = listOf("Klasa 6", "Klasa 7", "Klasa 8", "Wróć"),
                onOptionSelected = { className ->
                    if (className == "Wróć") {
                        currentState = AppState.SUBJECTS
                    } else {
                        selectedClass = className
                        currentState = AppState.TEXTBOOKS
                    }
                },
                modifier = modifier
            )
        }
        AppState.TEXTBOOKS -> {
            TextbookSelectionScreen(
                subjectName = selectedSubject,
                className = selectedClass,
                onTextbookSelected = {
                    currentState = AppState.CHAPTERS
                },
                onBackClicked = {
                    currentState = AppState.CLASSES
                },
                modifier = modifier
            )
        }
        AppState.CHAPTERS -> {
            ChapterSelectionScreen(
                onChapterSelected = { currentState = AppState.LEARNING_OPTIONS },
                onBackClicked = {
                    currentState = AppState.TEXTBOOKS
                },
                modifier = modifier
            )
        }
        AppState.LEARNING_OPTIONS -> {
            LearningOptionsScreen(
                onBackClicked = {
                    currentState = AppState.CHAPTERS
                },
                modifier = modifier
            )
        }
    }
}

data class ChapterItem(val title: String, val imageResId: Int?)

@Composable
fun ChapterSelectionScreen(
    onChapterSelected: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chapters = listOf(
        ChapterItem("Królestwo zwierząt", R.drawable.b_nowaera_6_1),
        ChapterItem("Bezkręgowce: parzydełkowce, płazińce, nicienie, pierścienice", R.drawable.b_nowaera_6_2),
        ChapterItem("Bezkręgowce: stawonogi i mięczaki", R.drawable.b_nowaera_6_3), // <-- tu podmień null na R.drawable.twoj_plik
        ChapterItem("Kręgowce zmiennocieplne: ryby, płazy i gady", R.drawable.b_nowaera_6_4), // <-- i tutaj
        ChapterItem("Kręgowce stałocieplne: ptaki i ssaki", R.drawable.b_nowaera_6_5) // <-- i tutaj
    )

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wybierz rozdział",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(top = 48.dp, bottom = 32.dp)
        )

        // LazyVerticalGrid pozwala ułożyć elementy w kafelki - tutaj ustawiłem 2 kolumny
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(chapters) { chapter ->
                Card(
                    modifier = Modifier
                        .height(140.dp)
                        .clickable { onChapterSelected() },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Jeśli przypisałeś obrazek (nie jest null), wyświetli go jako tło kafelka
                        if (chapter.imageResId != null) {
                            Image(
                                painter = painterResource(id = chapter.imageResId),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Text(
                            text = chapter.title,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            // Dodałem białe, lekko przezroczyste tło pod tekst,
                            // żeby nazwa rozdziału była czytelna na obrazku!
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        Button(
            onClick = onBackClicked,
            modifier = Modifier
                .padding(top = 32.dp, bottom = 32.dp)
                .width(200.dp)
        ) {
            Text(text = "< Wróć")
        }
    }
}

@Composable
fun TextbookSelectionScreen(
    subjectName: String,
    className: String,
    onTextbookSelected: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textbookCovers = listOf(
        R.drawable.biologia_gwo_6,
        R.drawable.biologia_mac_6,
        R.drawable.biologia_nowaera_6,
        R.drawable.biologia_wsip_6
    )

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wybierz podręcznik",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 48.dp, bottom = 8.dp)
        )
        Text(
            text = "$subjectName, $className",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(textbookCovers) { coverResId ->
                Card(
                    modifier = Modifier
                        .height(240.dp)
                        .clickable { onTextbookSelected() },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Image(
                        painter = painterResource(id = coverResId),
                        contentDescription = "Okładka książki",
                        modifier = Modifier.fillMaxHeight(),
                        contentScale = ContentScale.FillHeight
                    )
                }
            }
        }

        Button(
            onClick = onBackClicked,
            modifier = Modifier
                .padding(top = 32.dp)
                .width(200.dp)
        ) {
            Text(text = "< Wróć")
        }
    }
}

@Composable
fun MenuScreen(
    title: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        options.forEach { option ->
            Button(
                onClick = { onOptionSelected(option) },
                modifier = Modifier.width(200.dp)
            ) {
                if (option == "Wróć") {
                    Text(text = "< Wróć")
                } else {
                    Text(text = option)
                }
            }
        }
    }
}

@Composable
fun LearningOptionsScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLevel by remember { mutableStateOf("Fundamenty") }
    var selectedMode by remember { mutableStateOf("Puzzle") }

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
                isSelected = selectedLevel == "Fundamenty",
                onClick = { selectedLevel = "Fundamenty" },
                modifier = Modifier.weight(1f)
            )
            SelectionCard(
                title = "Idę dalej",
                subtitle = "rozwiń swoją wiedzę",
                isSelected = selectedLevel == "Idę dalej",
                onClick = { selectedLevel = "Idę dalej" },
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Wybierz formę nauki",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SelectionCard(
                title = "Puzzle",
                isSelected = selectedMode == "Puzzle",
                onClick = { selectedMode = "Puzzle" },
                modifier = Modifier.weight(1f)
            )
            SelectionCard(
                title = "Fiszki",
                isSelected = selectedMode == "Fiszki",
                onClick = { selectedMode = "Fiszki" },
                modifier = Modifier.weight(1f)
            )
        }
        
        SelectionCard(
            title = "Test",
            isSelected = selectedMode == "Test",
            onClick = { selectedMode = "Test" },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onBackClicked,
            modifier = Modifier
                .padding(top = 32.dp, bottom = 32.dp)
                .width(200.dp)
        ) {
            Text(text = "< Wróć")
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
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
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

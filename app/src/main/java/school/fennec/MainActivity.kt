package school.fennec

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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
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
            LabellingGameScreen(
                onBackToChapter = { currentState = AppState.CHAPTERS },
                onEndLearning = { currentState = AppState.MAIN_SETUP },
                modifier = modifier
            )
        }
    }
}

@Composable
fun MainSetupScreen(
    onNextClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubject by remember { mutableStateOf("Biologia") }
    var selectedClass by remember { mutableStateOf("Klasa 6") }
    // Domyślnie zaznaczamy pierwszy podręcznik, żeby uczeń wiedział jak to działa
    var selectedTextbook by remember { mutableStateOf<Int?>(R.drawable.biologia_gwo_6) } 

    val textbookCovers = listOf(
        R.drawable.biologia_gwo_6,
        R.drawable.biologia_mac_6,
        R.drawable.biologia_nowaera_6,
        R.drawable.biologia_wsip_6
    )

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
            listOf("Klasa 6", "Klasa 7", "Klasa 8").forEach { className ->
                SelectionCard(
                    title = className,
                    isSelected = selectedClass == className,
                    onClick = { selectedClass = className },
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
                    isSelected = selectedSubject == subject,
                    onClick = { selectedSubject = subject },
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
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(textbookCovers) { coverResId ->
                val isSelected = selectedTextbook == coverResId
                Card(
                    modifier = Modifier
                        .height(200.dp)
                        .clickable { selectedTextbook = coverResId },
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
                    // Zaznaczony podręcznik otrzymuje wyraźną, kolorową ramkę!
                    border = if (isSelected) BorderStroke(4.dp, MaterialTheme.colorScheme.primary) else null
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
        ChapterItem("Bezkręgowce: stawonogi i mięczaki", R.drawable.b_nowaera_6_3),
        ChapterItem("Kręgowce zmiennocieplne: ryby, płazy i gady", R.drawable.b_nowaera_6_4),
        ChapterItem("Kręgowce stałocieplne: ptaki i ssaki", R.drawable.b_nowaera_6_5)
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
                        .clickable { onChapterSelected() },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
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

@Composable
fun LearningOptionsScreen(
    onBackClicked: () -> Unit,
    onStartGameClicked: () -> Unit,
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
            onClick = onStartGameClicked,
            modifier = Modifier
                .padding(top = 48.dp)
                .width(200.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50), // Odcień zieleni z Material Design
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

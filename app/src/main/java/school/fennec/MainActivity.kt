package school.fennec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
    SUBJECTS, CLASSES, TEXTBOOKS, LEVELS
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
            AppState.LEVELS -> AppState.TEXTBOOKS
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
                options = listOf("Klasa 6", "Klasa 7", "Klasa 8"),
                onOptionSelected = { className ->
                    selectedClass = className
                    currentState = AppState.TEXTBOOKS
                },
                modifier = modifier
            )
        }
        AppState.TEXTBOOKS -> {
            TextbookSelectionScreen(
                subjectName = selectedSubject,
                className = selectedClass,
                onTextbookSelected = {
                    currentState = AppState.LEVELS
                },
                modifier = modifier
            )
        }
        AppState.LEVELS -> {
            // Ekran wyboru poziomu
            MenuScreen(
                title = "Wybór poziomu",
                options = listOf(), // Na razie puste, tu czekam na informacje od Ciebie
                onOptionSelected = { /* TODO */ },
                modifier = modifier
            )
        }
    }
}

@Composable
fun TextbookSelectionScreen(
    subjectName: String,
    className: String,
    onTextbookSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Lista z ID podanych przez Ciebie okładek książek
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
            text = "Wybierz podręcznik z którego się uczysz",
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
            // Marginesy sprawiają, że elementy po bokach "wystają", sugerując możliwość scrollowania
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
                Text(text = option)
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

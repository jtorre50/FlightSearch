package com.example.flightsearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flightsearch.ui.theme.FlightSearchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FlightSearchTheme {
                FlightSearchApp()
            }
        }
    }
}

@Composable
fun FlightSearchApp(
    flightViewModel: FlightViewModel = viewModel()
) {
    val state by flightViewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var searchField by remember {
        mutableStateOf(TextFieldValue(""))
    }

    LaunchedEffect(state.ready) {
        if (state.ready) {
            searchField = TextFieldValue(
                text = state.query,
                selection = TextRange(state.query.length)
            )
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Flight Search",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 16.dp)
            )

            OutlinedTextField(
                value = searchField,
                onValueChange = { newValue ->
                    val textChanged = newValue.text != searchField.text
                    searchField = newValue

                    if (textChanged) {
                        flightViewModel.onSearchChanged(newValue.text)
                    }
                },
                label = { Text("Departure airport name or code") },
                singleLine = true,
                enabled = state.ready,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (searchField.text.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                searchField = TextFieldValue("")
                                flightViewModel.onSearchChanged("")
                                focusManager.clearFocus()
                            }
                        ) {
                            Text("Clear")
                        }
                    }
                }
            )

            val departure = state.departure

            Text(
                text = when {
                    !state.ready -> "Loading..."
                    state.query.isBlank() -> "Favorite routes"
                    departure != null -> "Flights from ${departure.iataCode}"
                    else -> "Choose a departure airport"
                },
                style = MaterialTheme.typography.titleMedium
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    !state.ready -> Unit

                    state.query.isBlank() -> {
                        if (state.favorites.isEmpty()) {
                            item {
                                Text(
                                    "No favorites yet. Search for an airport " +
                                            "and save a route."
                                )
                            }
                        }

                        items(
                            items = state.favorites,
                            key = { it.id }
                        ) { favorite ->
                            RouteCard(
                                departure = favorite.departureCode,
                                destination = favorite.destinationCode,
                                isFavorite = true,
                                onToggleFavorite = {
                                    flightViewModel.toggleFavorite(
                                        favorite.departureCode,
                                        favorite.destinationCode
                                    )
                                }
                            )
                        }
                    }

                    departure != null -> {
                        items(
                            items = state.destinations,
                            key = { it.id }
                        ) { destination ->
                            val isFavorite = state.favorites.any {
                                it.departureCode == departure.iataCode &&
                                        it.destinationCode == destination.iataCode
                            }

                            RouteCard(
                                departure =
                                    "${departure.iataCode} — ${departure.name}",
                                destination =
                                    "${destination.iataCode} — ${destination.name}",
                                isFavorite = isFavorite,
                                onToggleFavorite = {
                                    flightViewModel.toggleFavorite(
                                        departure.iataCode,
                                        destination.iataCode
                                    )
                                }
                            )
                        }
                    }

                    else -> {
                        if (state.suggestions.isEmpty()) {
                            item {
                                Text("No matching airports.")
                            }
                        }

                        items(
                            items = state.suggestions,
                            key = { it.id }
                        ) { airport ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchField = TextFieldValue(
                                            text = airport.iataCode,
                                            selection =
                                                TextRange(airport.iataCode.length)
                                        )
                                        flightViewModel.selectAirport(airport)
                                        focusManager.clearFocus()
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement =
                                        Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = airport.iataCode,
                                        style =
                                            MaterialTheme.typography.titleMedium
                                    )
                                    Text(airport.name)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteCard(
    departure: String,
    destination: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "DEPARTURE",
                style = MaterialTheme.typography.labelSmall
            )
            Text(departure)

            Text(
                text = "DESTINATION",
                style = MaterialTheme.typography.labelSmall
            )
            Text(destination)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onToggleFavorite) {
                    Text(
                        if (isFavorite) "★ Remove favorite"
                        else "☆ Save favorite"
                    )
                }
            }
        }
    }
}
package com.example.flightsearch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flightsearch.data.Airport
import com.example.flightsearch.data.Favorite
import com.example.flightsearch.data.FlightDatabase
import com.example.flightsearch.data.SearchPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class FlightUiState(
    val query: String = "",
    val departure: Airport? = null,
    val suggestions: List<Airport> = emptyList(),
    val destinations: List<Airport> = emptyList(),
    val favorites: List<Favorite> = emptyList(),
    val ready: Boolean = false
)

private data class SearchState(
    val query: String = "",
    val departure: Airport? = null,
    val ready: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class FlightViewModel(application: Application) :
    AndroidViewModel(application) {

    private val dao = FlightDatabase
        .getDatabase(application)
        .flightDao()

    private val preferences = SearchPreferences(application)
    private val search = MutableStateFlow(SearchState())
    private val pendingSearches = Channel<String>(Channel.CONFLATED)
    private val favoriteMutex = Mutex()

    val uiState = search.flatMapLatest { state ->
        val suggestions =
            if (state.query.isNotBlank() && state.departure == null) {
                dao.searchAirports(state.query.trim())
            } else {
                flowOf(emptyList<Airport>())
            }

        val destinations = state.departure?.let {
            dao.getDestinations(it.iataCode)
        } ?: flowOf(emptyList<Airport>())

        combine(
            suggestions,
            destinations,
            dao.getFavorites()
        ) { airports, routes, favorites ->
            FlightUiState(
                query = state.query,
                departure = state.departure,
                suggestions = airports,
                destinations = routes,
                favorites = favorites,
                ready = state.ready
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FlightUiState()
    )

    init {
        viewModelScope.launch {
            val savedQuery = preferences.searchQuery.first()
            search.value = SearchState(
                query = savedQuery,
                ready = true
            )

            for (query in pendingSearches) {
                preferences.saveSearchQuery(query)
            }
        }
    }

    fun onSearchChanged(query: String) {
        search.value = SearchState(
            query = query,
            ready = true
        )
        pendingSearches.trySend(query)
    }

    fun selectAirport(airport: Airport) {
        search.value = SearchState(
            query = airport.iataCode,
            departure = airport,
            ready = true
        )
        pendingSearches.trySend(airport.iataCode)
    }

    fun toggleFavorite(
        departureCode: String,
        destinationCode: String
    ) {
        viewModelScope.launch {
            favoriteMutex.withLock {
                val exists = dao.getFavorites().first().any {
                    it.departureCode == departureCode &&
                            it.destinationCode == destinationCode
                }

                if (exists) {
                    dao.removeFavorite(
                        departureCode,
                        destinationCode
                    )
                } else {
                    dao.addFavorite(
                        Favorite(
                            departureCode = departureCode,
                            destinationCode = destinationCode
                        )
                    )
                }
            }
        }
    }
}
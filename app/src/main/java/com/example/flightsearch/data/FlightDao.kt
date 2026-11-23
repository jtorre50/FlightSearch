package com.example.flightsearch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {

    @Query("""
        SELECT * FROM airport
        WHERE iata_code LIKE '%' || :query || '%'
           OR name LIKE '%' || :query || '%'
        ORDER BY passengers DESC
    """)
    fun searchAirports(query: String): Flow<List<Airport>>

    @Query("""
        SELECT * FROM airport
        WHERE iata_code != :departureCode
        ORDER BY passengers DESC
    """)
    fun getDestinations(departureCode: String): Flow<List<Airport>>

    @Query("SELECT * FROM favorite ORDER BY id DESC")
    fun getFavorites(): Flow<List<Favorite>>

    @Insert
    suspend fun addFavorite(favorite: Favorite)

    @Query("""
        DELETE FROM favorite
        WHERE departure_code = :departureCode
          AND destination_code = :destinationCode
    """)
    suspend fun removeFavorite(
        departureCode: String,
        destinationCode: String
    )
}
package com.p2lem8dev.internetRadio.database.radio.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation

@Dao
abstract class RadioStationsDao {

    @Query("SELECT * FROM radio_stations")
    abstract fun getAllStationsLiveData(): LiveData<List<RadioStation>>

    @Query("SELECT * FROM radio_stations WHERE isFavorite=1")
    abstract fun getAllFavoriteStationsLiveData(): LiveData<List<RadioStation>>

    @Query("SELECT * FROM radio_stations")
    abstract suspend fun selectAll(): List<RadioStation>

    @Query("SELECT * FROM radio_stations LIMIT :count")
    abstract suspend fun selectAll(count: Int): List<RadioStation>

    @Query("SELECT * FROM radio_stations LIMIT 1")
    abstract suspend fun first(): RadioStation

    @Query("SELECT * FROM radio_stations ORDER BY id DESC LIMIT 1")
    abstract suspend fun last(): RadioStation

    @Query("SELECT * FROM radio_stations WHERE isFavorite=1")
    abstract suspend fun selectAllFavorite(): List<RadioStation>

    @Query("SELECT * FROM radio_stations WHERE isFavorite=0")
    abstract suspend fun selectAllNotFavorite(): List<RadioStation>

    @Query("SELECT * FROM radio_stations ORDER BY title")
    abstract suspend fun selectAllByTitle(): List<RadioStation>

    @Query("SELECT * FROM radio_stations ORDER BY listeners")
    abstract suspend fun selectAllByListeners(): List<RadioStation>

    @Query("SELECT * FROM radio_stations WHERE stationId=:stationId")
    abstract suspend fun findByStationId(stationId: String): RadioStation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(station: RadioStation)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(station: RadioStation)

    suspend fun insertAll(stations: List<RadioStation>) {
        stations.forEach { insert(it) }
    }

    suspend fun updateAll(stations: List<RadioStation>) {
        stations.forEach { update(it) }
    }

    @Query("DELETE FROM radio_stations WHERE stationId=:stationId")
    abstract suspend fun delete(stationId: String)

    @Query("DELETE FROM radio_stations")
    abstract suspend fun deleteAll()

}
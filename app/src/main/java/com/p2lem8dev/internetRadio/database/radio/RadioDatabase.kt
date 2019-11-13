package com.p2lem8dev.internetRadio.database.radio

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.p2lem8dev.internetRadio.database.converters.JsonConverters
import com.p2lem8dev.internetRadio.database.radio.dao.RadioStationsDao
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation


@Database(
    entities = [
        RadioStation::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(JsonConverters::class)
abstract class RadioDatabase : RoomDatabase() {

    abstract fun getRadioStationsDao(): RadioStationsDao

    companion object {
        private const val DATABASE_NAME = "radio_stations_db"

        private var mInstance: RadioDatabase? = null

        fun getInstance(context: Context): RadioDatabase {
            if (mInstance != null) {
                return mInstance!!
            }
            mInstance = Room.databaseBuilder(
                context,
                RadioDatabase::class.java,
                DATABASE_NAME
            ).build()
            return mInstance!!
        }
    }
}
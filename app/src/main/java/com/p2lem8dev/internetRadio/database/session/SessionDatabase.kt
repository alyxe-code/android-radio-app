package com.p2lem8dev.internetRadio.database.session

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.p2lem8dev.internetRadio.database.converters.JsonConverters
import com.p2lem8dev.internetRadio.database.session.dao.SessionDao
import com.p2lem8dev.internetRadio.database.session.entities.Session

@Database(entities = [Session::class], version = 1, exportSchema = false)
@TypeConverters(JsonConverters::class)
abstract class SessionDatabase : RoomDatabase() {

    abstract fun getSessionDao(): SessionDao

    companion object {
        private const val DATABASE_NAME = "session_db"

        private var mInstance: SessionDatabase? = null

        fun getInstance(context: Context): SessionDatabase {
            if (mInstance != null) return mInstance!!
            mInstance = Room.databaseBuilder(
                context,
                SessionDatabase::class.java,
                DATABASE_NAME
            ).build()
            return mInstance!!
        }
    }
}
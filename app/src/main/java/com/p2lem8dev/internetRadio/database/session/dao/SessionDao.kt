package com.p2lem8dev.internetRadio.database.session.dao

import androidx.room.*
import com.p2lem8dev.internetRadio.database.session.entities.Session
import java.util.*

@Dao
abstract class SessionDao {

    @Query("SELECT * FROM session ORDER BY createdAt LIMIT 1")
    abstract suspend fun getCurrentSession(): Session?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(session: Session)

    @Update
    abstract suspend fun update(session: Session)

    @Query("UPDATE session SET lastSyncDate=:syncDate WHERE id=:currentSessionId")
    abstract suspend fun updateSyncDate(currentSessionId: Long, syncDate: Date = Date())

    @Query("DELETE FROM session WHERE isValid=0")
    abstract suspend fun deleteAllInvalidSessions()

    @Query("DELETE FROM session")
    abstract suspend fun deleteAllSessionsData()
}
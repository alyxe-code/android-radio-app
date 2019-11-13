package com.p2lem8dev.internetRadio.database.session.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "session")
public class Session (
    var isValid: Boolean,
    val createdAt: Date,
    var invalidationDate: Date? = null,
    var lastRunningStationId: String?,
    var isPlaying: Boolean = false,
    var lastSyncDate: Date?,
    var username: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 1
}
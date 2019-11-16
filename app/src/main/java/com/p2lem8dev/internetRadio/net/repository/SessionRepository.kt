package com.p2lem8dev.internetRadio.net.repository

import com.p2lem8dev.internetRadio.database.session.dao.SessionDao
import com.p2lem8dev.internetRadio.database.session.entities.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class SessionRepository(private val sessionDao: SessionDao) {

    /**
     * Start new session only ignoring existence and validity
     */
    private suspend fun startNewSessionForce(): Session {
        var session: Session? = null
        withContext(context = Dispatchers.IO) {
            session = sessionDao.getCurrentSession()
            if (session != null) {
                session!!.isValid = false
                sessionDao.update(session!!)
            }
            sessionDao.insert(
                Session(
                    isValid = true,
                    createdAt = Date(),
                    lastRunningStationId = null,
                    lastSyncDate = null,
                    username = UUID.randomUUID().toString(),
                    isPlaying = false,
                    invalidationDate = null
                )
            )
            session = sessionDao.getCurrentSession()!!
        }
        return session!!
    }

    /**
     * Start new session if current is invalid or doesn't exist
     */
    public suspend fun startNewSession(): Boolean {
        var result = false
        withContext(context = Dispatchers.IO) {
            val currentSession = sessionDao.getCurrentSession()
            if (currentSession == null || !currentSession.isValid) {
                startNewSessionForce()
                result = true
            }
        }
        return result
    }

    /**
     * Get current session. If current session doesn't exist
     * or invalid - it will start new session
     */
    public suspend fun getCurrentSession(): Session {
        startNewSession()
        var currentSession: Session? = null
        withContext(context = Dispatchers.IO) {
            currentSession = sessionDao.getCurrentSession()
        }
        return currentSession!!
    }

    /**
     * Validate current session
     */
    public suspend fun invalidateSession() = withContext(context = Dispatchers.IO) {
        getCurrentSession().let {
            it.isValid = false
            it.invalidationDate = Date()
            sessionDao.update(it)
        }
    }

    /**
     * Update sync date of current session
     */
    public suspend fun updateSyncDate() = withContext(context = Dispatchers.IO) {
        val currentSession = getCurrentSession()
        sessionDao.updateSyncDate(currentSession.id, Date())
    }

    /**
     * Check whether the sync date of current date is invalid
     */
    public suspend fun isSyncDateValid(): Boolean {
        var result = false
        withContext(context = Dispatchers.IO) {
            val currentSession = getCurrentSession()
            if (currentSession.lastSyncDate == null) {
                return@withContext
            }
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DATE, 7)
            result = currentSession.lastSyncDate!!.before(calendar.time)
        }
        return result
    }

    /**
     * Rename current user
     */
    public suspend fun changeUsername(username: String) = withContext(context = Dispatchers.IO) {
        sessionDao.update(getCurrentSession().apply { this.username = username })
    }

    /**
     * Set session stationId
     */
    public suspend fun changeStationId(stationId: String) = withContext(context = Dispatchers.IO) {
        sessionDao.update(getCurrentSession().apply { lastRunningStationId = stationId })
    }

    public suspend fun updateLastRunningStation(stationId: String) = withContext(context = Dispatchers.IO) {
        getCurrentSession().let {
            it.lastRunningStationId = stationId
            sessionDao.update(it)
        }
    }

    public suspend fun setRadioRunning(stationId: String) = withContext(context = Dispatchers.IO) {
        getCurrentSession().let {
            it.lastRunningStationId = stationId
            it.isPlaying = true
            sessionDao.update(it)
        }
    }

    public suspend fun setRadioStopped(stationId: String?) = withContext(context = Dispatchers.IO) {
        getCurrentSession().let {
            it.lastRunningStationId = stationId
            it.isPlaying = false
            sessionDao.update(it)
        }
    }

    public suspend fun isPlaying() = getCurrentSession().isPlaying

    public suspend fun whichPlaying(): String? {
        return getCurrentSession().lastRunningStationId
    }

    companion object {
        private var mInstance: SessionRepository? = null

        fun create(sessionDao: SessionDao) {
            mInstance = SessionRepository(sessionDao)
        }

        fun get(): SessionRepository {
            requireNotNull(mInstance)
            return mInstance!!
        }
    }
}
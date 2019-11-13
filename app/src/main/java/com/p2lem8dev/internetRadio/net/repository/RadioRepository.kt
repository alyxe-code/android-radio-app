package com.p2lem8dev.internetRadio.net.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.p2lem8dev.internetRadio.database.radio.RadioDatabase
import com.p2lem8dev.internetRadio.database.radio.dao.RadioStationsDao
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.net.api.RadioBaseResponse
import com.p2lem8dev.internetRadio.net.api.RadioTochkaAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.internal.ThreadSafeHeap
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URL


class RadioRepository(
    private val mApplicationContext: Context,
    private val mRadioStationsDao: RadioStationsDao,
    private val radioTochkaAPI: RadioTochkaAPI
) {

    private val mSupportedURLProtocols = listOf("HTTP", "HTTPS")

    suspend fun getCountStations(canContinue: suspend () -> Boolean): Int {
        waitWhile(canContinue)
        return radioTochkaAPI.getCountRadios().items.toInt()
    }

    private suspend fun waitWhile(canContinue: suspend () -> Boolean, sleepTime: Long = 100L) {
        while (!canContinue()) {
            Thread.sleep(sleepTime)
        }
    }

    /**
     * Fetch all allStations on API Server
     * Determine illegal allStations and remove it
     * Add all received allStations
     * @param filter Advanced filter
     * @param onNextValid Will be called on each fetch iteration
     */
    fun sync(
        filter: ((station: RadioStation) -> Boolean)? = null,
        onNextValid: ((station: RadioStation) -> Unit)? = null,
        onNextIteration: ((RadioBaseResponse?, index: Int, count: Int) -> Unit)? = null,
        onFinish: ((start: Long, finish: Long) -> Unit)? = null,
        canContinue: (suspend () -> Boolean)
    ) {
        GlobalScope.launch(context = Dispatchers.IO) {
            val startTimestamp = System.currentTimeMillis() / 1000

            var indexPage = 0
            var indexItem = 0

            waitWhile(canContinue)
            val count = radioTochkaAPI.getCountRadios().items.toInt()
            val receivedStations = arrayListOf<RadioStation>()

            val dao = RadioDatabase
                .getInstance(mApplicationContext)
                .getRadioStationsDao()

            while (true) {
                var items = loadItemsFromPage(
                    indexPage,
                    true,
                    mApplicationContext.filesDir.absolutePath,
                    onNextIteration = {
                        onNextIteration?.invoke(
                            it,
                            indexItem,
                            count
                        )
                        indexItem += 1
                    },
                    onNext = { onNextValid?.invoke(it) },
                    canContinue = canContinue
                )

                if (items != null) {
                    if (filter != null) {
                        items = items.filter(filter)
                    }

                    receivedStations.addAll(items)
                    indexPage += 1
                }

                if (indexItem >= count) break
            }

            val toRemove = dao.selectAll()
                .filter { db -> receivedStations.any { it.stationId == db.stationId } }
                .map { it.stationId }
            toRemove.forEach { dao.delete(it) }
            dao.insertAll(receivedStations)
            onFinish?.invoke(startTimestamp, System.currentTimeMillis() / 1000)
        }
    }

    /**
     * Check if URL is valid
     */
    private suspend fun isValid(url: URL): Boolean {
        var valid = false
        try {
            if (!mSupportedURLProtocols.contains(url.protocol.toUpperCase())) {
                return false
            }
            withContext(context = Dispatchers.IO) {
                valid = try {
                    url.openConnection().contentType == "audio/mpeg"
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            Log.d(LOG_TAG, "IS_VALID | EXCEPTION | ${e.message} \n${e.stackTrace.joinToString("\n")}")
        }
        return valid
    }

    /**
     * Load list of RadioStation from https://radio-tochka.ru.
     * Then it implement some simple filtering and then fills data.
     * @param page Page Index
     * @param loadImages Should it download either image
     * @param saveImagesPath Directory where to save downloaded images
     * @param onNext Returns filled item
     * @return List of items valid RadioStation
     */
    private suspend fun loadItemsFromPage(
        page: Int,
        loadImages: Boolean = true,
        saveImagesPath: String,
        onNextIteration: ((RadioBaseResponse?) -> Unit)? = null,
        onNext: (station: RadioStation) -> Unit,
        canContinue: suspend () -> Boolean,
        sleepTime: Long = 100L
    ): List<RadioStation>? {
        var availableStations: ArrayList<RadioStation>? = null
        withContext(context = Dispatchers.IO) {
            waitWhile(canContinue)

            val pageItems = ArrayList<RadioBaseResponse>()

            while (true) {
                try {
                    pageItems.addAll(radioTochkaAPI.getPage(page))
                    break
                } catch(e: Exception) {
                    Log.d(LOG_TAG, "GET_PAGE | EXCEPTION | ${e.message}")
                    waitWhile(canContinue)
                    continue
                }
            }

            if (pageItems.isEmpty()) {
                availableStations = null
                return@withContext
            }

            for (item in pageItems) {
                waitWhile(canContinue)
                try {
                    onNextIteration?.invoke(item)
                    val links = item.links.filter { link ->
                        val url: URL
                        try {
                            waitWhile(canContinue)
                            url = URL(link.url)
                        } catch (e: Exception) {
                            return@filter false
                        }
                        waitWhile(canContinue)
                        return@filter isValid(url)
                    }
                    if (links.isEmpty()) continue

                    waitWhile(canContinue)
                    val radio = radioTochkaAPI.getRadio(item.id) ?: continue
                    val station = RadioStation(
                        stationId = radio.stationId,
                        title = radio.title,
                        country = radio.country,
                        region = radio.region,
                        city = radio.city,
                        links = links.map { it.url } as ArrayList<String>,
                        listeners = radio.listeners.toInt(),
                        views = radio.views.toInt(),
                        playerStream = radio.playerStream,
                        language = radio.language,
                        genres = radio.genres.map { it.genre } as ArrayList<String>,
                        voted = radio.voted.toInt(),
                        isFavorite = false,
                        imageUrl = null
                    )

                    onNext(station)

                    if (loadImages) {
                        val destDir = File(saveImagesPath)
                        if (!destDir.exists())
                            throw IOException("File $destDir not found")

                        waitWhile(canContinue)
                        val response = radioTochkaAPI.getStationImage(station.stationId)
                        val contentType = response.contentType()
                        if (contentType?.type() == "image") {
                            val path =
                                "$saveImagesPath/${station.stationId}.${contentType.subtype()}"
                            val imageFile = File(path)
                            if (imageFile.exists()) {
                                imageFile.delete()
                            }
                            imageFile.writeBytes(response.bytes())
                            station.imageUrl = imageFile.absolutePath
                        }
                    }

                    if (availableStations == null) {
                        availableStations = ArrayList()
                    }

                    availableStations?.add(station)
                } catch (e: Exception) {
                    // skip iteration
                    onNextIteration?.invoke(null)
                }
            }

        }

        return availableStations
    }

    fun getAllStationsLiveData() = mRadioStationsDao.getAllStationsLiveData()

    fun getAllFavoriteStationsLiveData() = mRadioStationsDao.getAllFavoriteStationsLiveData()

    fun setFavorite(stationId: String, value: Boolean) {
        GlobalScope.launch {
            mRadioStationsDao.findByStationId(stationId)?.let {
                it.isFavorite = value
                mRadioStationsDao.update(it)
            }
        }
    }

    suspend fun getAllStations(): List<RadioStation> {
        return mRadioStationsDao.selectAll()
    }

    suspend fun getAllFavoriteStations(): List<RadioStation> {
        return mRadioStationsDao.selectAllFavorite()
    }

    suspend fun findByStationId(stationId: String): RadioStation? {
        return mRadioStationsDao.findByStationId(stationId)
    }

    companion object {
        private const val LOG_TAG = "RADIO_REPO"

        private var mInstance: RadioRepository? = null

        fun get(): RadioRepository {
            requireNotNull(mInstance)
            return mInstance!!
        }

        fun create(
            context: Context,
            stationsDao: RadioStationsDao,
            radioTochkaAPI: RadioTochkaAPI
        ) {
            mInstance = RadioRepository(context, stationsDao, radioTochkaAPI)
        }
    }
}
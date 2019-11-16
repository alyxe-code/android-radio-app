package com.p2lem8dev.internetRadio.net.repository

import android.content.Context
import android.util.Log
import android.webkit.URLUtil
import com.p2lem8dev.internetRadio.app.service.sync.extractors.ConverterFactory
import com.p2lem8dev.internetRadio.app.service.sync.extractors.M3UConverterFactory
import com.p2lem8dev.internetRadio.app.service.sync.extractors.PLSConverterFactory
import com.p2lem8dev.internetRadio.app.service.sync.extractors.URLConverterFactory
import com.p2lem8dev.internetRadio.database.radio.dao.RadioStationsDao
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.database.radio.factory.RadioStationFactory
import com.p2lem8dev.internetRadio.net.api.Link
import com.p2lem8dev.internetRadio.net.api.BaseRadioInfo
import com.p2lem8dev.internetRadio.net.api.FullRadioInfo
import com.p2lem8dev.internetRadio.net.api.RadioAPI
import com.p2lem8dev.internetRadio.net.utils.InternetConnectionTester
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.ResponseBody
import java.io.File

class RadioStationRepository(
    private val stationsDao: RadioStationsDao,
    private val api: RadioAPI,
    private val urlConverterFactories: List<ConverterFactory>? = listOf(
        M3UConverterFactory(),
        PLSConverterFactory()
    )
) {

    private var _countValidStations: Int = 0
    var countValidStations
        get() = _countValidStations
        private set(value) {
            _countValidStations = value
        }

    fun getAllStationsLiveData() = stationsDao.getAllStationsLiveData()

    fun getAllFavoriteStationsLiveData() = stationsDao.getAllFavoriteStationsLiveData()

    suspend fun getAllStations() = stationsDao.selectAll()

    suspend fun getAllFavoriteStations() = stationsDao.selectAllFavorite()

    suspend fun setFavorite(stationId: String, value: Boolean) =
        withContext(context = Dispatchers.IO) {
            stationsDao.findByStationId(stationId)?.let {
                it.isFavorite = value
                stationsDao.update(it)
            }
        }

    suspend fun invertFavorite(stationId: String) = withContext(context = Dispatchers.IO) {
        stationsDao.findByStationId(stationId)?.let {
            it.isFavorite = !it.isFavorite
            stationsDao.update(it)
        }
    }

    suspend fun findStation(stationId: String): RadioStation? {
        var station: RadioStation? = null
        withContext(context = Dispatchers.IO) {
            station = stationsDao.findByStationId(stationId)
        }
        return station
    }

    /**
     * Synchronize database with API stored radio stations
     * @param imagesSaveDir Destination path to save images
     * @param onLoad Every time when a station has been found and validated
     * @param onSave Every time when a station is going to be saved to database
     * @param updateIfExists If station row was found in database - ignore
     */
    suspend fun sync(
        imagesSaveDir: String,
        updateIfExists: Boolean = true,
        onLoad: ((station: BaseRadioInfo) -> Unit)? = null,
        onSave: ((station: RadioStation) -> Unit)? = null
    ) {
        stationsDao.deleteAll()

        // when a station has been found it must be filled and added to database
        loadAllStations(true) saveStation@{
            try {
                onLoad?.invoke(it)

                val station = fillRadioStation(it.id) ?: return@saveStation
                val dbStation = stationsDao.findByStationId(it.id)
                if (dbStation != null) {
                    if (!updateIfExists) {
                        return@saveStation
                    }
                    station.id = dbStation.id
                    station.isFavorite = dbStation.isFavorite
                }

                val imagePath = downloadStationImage(it.id, imagesSaveDir)
                if (imagePath != null) {
                    station.imageUrl = imagePath
                }

                onSave?.invoke(station)
                createOrUpdateStation(station)
            } catch (e: Exception) {
                Log.wtf(
                    "SYNC_SAVE",
                    "Station#${it.id} ${it.title} thrown exception ${e.message} \n" +
                            e.stackTrace.joinToString("\n")
                )
            }
        }
    }

    /**
     * Load all stations via API service
     * For each found station it launches a new coroutine
     */
    suspend fun loadAllStations(
        onlyRunning: Boolean = false,
        downloadImages: Boolean = true,
        downloadDestinationDirectory: String? = null,
        onNext: (suspend (station: BaseRadioInfo) -> Unit)? = null
    ) {
        var pageIndex = 0
        while (true) {
            var pageStations: List<BaseRadioInfo>
            while (true) {
                try {
                    pageStations = api.getPage(pageIndex)
                    pageIndex++
                    break
                } catch (e: Exception) {
                    InternetConnectionTester.waitConnection()
                }
            }
            Log.d("SYNC", "Load page $pageIndex | found ${pageStations.size} stations")
            if (pageStations.isEmpty()) break
            pageStations.forEach { station ->
                GlobalScope.launch {
                    Log.d("SYNC", "Loading ${station.id} ${station.title}")
                    try {
                        val links = arrayListOf<Link>()
                        station.links.forEach { link ->
                            Log.d("SYNC", "Trying link ${link.url}")
                            URLConverterFactory.extract(
                                link.url,
                                onlyRunning,
                                urlConverterFactories
                            )
                                ?.minBy { URLUtil.isHttpsUrl(it) }
                                ?.let { links.add(Link(it, link.bitrate, link.app)) }
                        }
                        if (links.isNotEmpty()) {
                            Log.d("SYNC", "Saving station ${station.id} ${station.title}")
                            station.links = links

                            val radioStation = RadioStationFactory.fromBaseInfo(station)

                            if (downloadImages && downloadDestinationDirectory != null) {
                                val imageUrl = downloadStationImage(station.id, downloadDestinationDirectory)
                                radioStation.imageUrl = imageUrl
                            }

                            createOrUpdateStation(radioStation)
                            onNext?.invoke(station)
                        } else {
                            Log.d("SYNC", "Ignore station ${station.id} ${station.title}")
                        }
                    } catch (e: Exception) {
                        Log.d(
                            "SYNC",
                            "LOAD | Station ${station.id} thrown exception\n" +
                                    "Message: ${e.message}\n" +
                                    e.stackTrace.joinToString("\n")
                        )
                    }
                }
            }
            Thread.sleep(1)
        }
    }


    /**
     * Get full radio station data
     */
    private suspend fun fillRadioStation(stationId: String): RadioStation? {
        var radio: FullRadioInfo
        while (true) {
            try {
                radio = api.getRadio(stationId) ?: return null
                break
            } catch (e: Exception) {
                InternetConnectionTester.waitConnection()
            }
        }

        val station = RadioStationFactory.from(radio)
        createOrUpdateStation(station)
        return station
    }

    /**
     * Download station image
     * @param stationId Id get download image
     * @param destPath Destination path to save image
     * @return Downloaded image path
     */
    private suspend fun downloadStationImage(
        stationId: String,
        destPath: String,
        force: Boolean = false
    ): String? {
        val dir = File(destPath)
        // check destination file exists
        if (!dir.exists()) dir.mkdir()
        // validate destination file is directory
        if (dir.exists() && !dir.isDirectory) {
            if (force) {
                dir.delete()
                dir.mkdir()
            } else return null
        }
        // get image via API
        var stationImage: ResponseBody
        while (true) {
            try {
                stationImage = api.getStationImage2(stationId)
                break
            } catch (e: Exception) {
                // wait connection and retry to download image
                InternetConnectionTester.waitConnection()
            }
        }
        // validate received content type
        var contentType: MediaType?
        while (true) {
            try {
                contentType = stationImage.contentType()
                break
            } catch (e: Exception) {
                InternetConnectionTester.waitConnection()
            }
        }
        if (contentType == null || contentType.type() != "image") {
            return null
        }
        // save image on disk
        var filename: String? = "$destPath/$stationId.${contentType.subtype()}"
        withContext(context = Dispatchers.IO) {
            try {
                File(filename!!).writeBytes(stationImage.bytes())
            } catch (e: Exception) {
                filename = null
            }
        }
        return filename
    }

    /**
     * Update or create a radio station in database
     * @return True when created
     */
    private suspend fun createOrUpdateStation(station: RadioStation): Boolean {
        val dbStation = stationsDao.findByStationId(station.stationId)
        if (dbStation == null) {
            stationsDao.insert(station)
            return true
        } else {
            station.id = dbStation.id
            station.isFavorite = dbStation.isFavorite
            stationsDao.update(station)
            return false
        }
    }

    companion object {
        private var mInstance: RadioStationRepository? = null

        fun create(
            applicationContext: Context,
            stationsDao: RadioStationsDao,
            api: RadioAPI
        ) {
            mInstance = RadioStationRepository(stationsDao, api)
        }

        fun get(): RadioStationRepository {
            requireNotNull(mInstance)
            return mInstance!!
        }
    }
}
package com.p2lem8dev.internetRadio.net.repository

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


    suspend fun loadRadiosFromAllPages(
        onlyRunning: Boolean,
        saveImagesDirectory: String,
        onNextFilter: ((station: BaseRadioInfo) -> Unit)? = null,
        onNextDownloadImage: ((station: RadioStation) -> Unit)? = null,
        onNextSave: (suspend (station: RadioStation) -> Unit)?
    ) {
        var pageIndex = 0
        while (true) {
            val radios = loadRadiosFromPage(pageIndex) ?: break
            Log.d("SYNC", "Found page $pageIndex ${radios.size}")
            pageIndex++

            GlobalScope.launch {
                handleLoadedPage(radios, onlyRunning, saveImagesDirectory, onNextFilter, onNextDownloadImage, onNextSave)
            }
        }
    }

    private suspend fun loadRadiosFromPage(page: Int): List<BaseRadioInfo>? {
        var radios = listOf<BaseRadioInfo>()
        withContext(context = Dispatchers.IO) {
            radios = api.getPage(page)
        }
        return if (radios.isEmpty()) null
        else radios
    }

    private suspend fun handleLoadedPage(
        pageRadios: List<BaseRadioInfo>,
        onlyRunning: Boolean,
        saveImagesDirectory: String,
        onNextFilter: ((station: BaseRadioInfo) -> Unit)? = null,
        onNextDownloadImage: ((station: RadioStation) -> Unit)? = null,
        onNextSave: (suspend (station: RadioStation) -> Unit)?
    ) {
        pageRadios
            .mapNotNull {
                Log.d("SYNC_HANDLE", "Filter ${it.title}")
                onNextFilter?.invoke(it)
                filterLoadedRadios(it, onlyRunning)
            }
            .map {
                Log.d("SYNC_HANDLE", "Create from base ${it.title}")
                RadioStationFactory.fromBaseRadioInfo(it)
            }
            .map {
                Log.d("SYNC_HANDLE", "Download image ${it.title}")
                onNextDownloadImage?.invoke(it)
                it.apply { imageUrl = downloadRadioImage(it.stationId, saveImagesDirectory) }
            }
            .forEach {
                Log.d("SYNC_HANDLE", "Saving ${it.title}")
                createOrUpdateStationBase(it)
                onNextSave?.invoke(it)
            }
    }

    private suspend fun filterLoadedRadios(
        baseRadio: BaseRadioInfo,
        onlyRunning: Boolean
    ): BaseRadioInfo? {
        val links = arrayListOf<Link>()
        baseRadio.links.forEach { link ->
            Log.d("SYNC", "Trying link ${link.url}")
            withContext(context = Dispatchers.IO) {
                URLConverterFactory.extract(
                    link.url,
                    onlyRunning,
                    urlConverterFactories
                )
                    ?.minBy { URLUtil.isHttpsUrl(it) }
                    ?.let { links.add(Link(it, link.bitrate, link.app)) }
            }
        }
        return if (links.isEmpty()) null
        else baseRadio
    }


    private var canContinueLoadPages = true


    private var toDatabaseRadios = ArrayList<BaseRadioInfo>()

    private fun addToDBQueue(
        radio: BaseRadioInfo,
        downloadImages: Boolean = true,
        downloadDestinationDirectory: String? = null,
        onNext: (suspend (station: BaseRadioInfo) -> Unit)?
    ) {
        toDatabaseRadios.add(radio)
        if (toDatabaseRadios.size == 10) {
            GlobalScope.launch {
                val saveItems = toDatabaseRadios
                toDatabaseRadios.forEach {
                    GlobalScope.launch(context = Dispatchers.IO) {
                        val radioStation = RadioStationFactory.fromBaseRadioInfo(it)

                        if (downloadImages && downloadDestinationDirectory != null) {
                            val imageUrl = downloadRadioImage(it.id, downloadDestinationDirectory)
                            radioStation.imageUrl = imageUrl
                        }

                        createOrUpdateStation(radioStation)
                        onNext?.invoke(it)
                    }
                }
            }
            toDatabaseRadios = arrayListOf()
        }
    }


    /**
     * Load full version of radio station
     * May consume some time
     * After station was loaded it will be saved
     */
    suspend fun loadOrUpdateRadioStation(stationId: String): RadioStation? {
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
    private suspend fun downloadRadioImage(
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
        var filename: String? = null
        withContext(context = Dispatchers.IO) {
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
                return@withContext
            }
            // save image on disk
            filename = "$destPath/$stationId.${contentType.subtype()}"
            withContext(context = Dispatchers.IO) {
                try {
                    File(filename!!).writeBytes(stationImage.bytes())
                } catch (e: Exception) {
                    filename = null
                    return@withContext
                }
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
        return if (dbStation == null) {
            stationsDao.insert(station)
            true
        } else {
            station.id = dbStation.id
            station.isFavorite = dbStation.isFavorite
            if (station.imageUrl == null) {
                station.imageUrl = dbStation.imageUrl
            }
            stationsDao.update(station)
            false
        }
    }

    private suspend fun createOrUpdateStationBase(station: RadioStation): Boolean {
        val dbStation = stationsDao.findByStationId(station.stationId)
        return if (dbStation == null) {
            stationsDao.insert(station)
            true
        } else {
            dbStation.title = station.title
            dbStation.links = station.links
            dbStation.imageUrl = station.imageUrl
            stationsDao.update(dbStation)
            false
        }
    }

    suspend fun deleteByStationId(stationId: String) {
        stationsDao.delete(stationId)
    }

    companion object {
        private var mInstance: RadioStationRepository? = null

        fun create(
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
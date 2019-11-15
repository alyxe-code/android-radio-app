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
import com.p2lem8dev.internetRadio.net.api.RadioTochkaAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RadioStationRepository(
    private val stationsDao: RadioStationsDao,
    private val api: RadioTochkaAPI,
    private val urlConverterFactories: List<ConverterFactory>? = listOf(
        M3UConverterFactory(),
        PLSConverterFactory()
    )
) {

    /**
     * Synchronize database with API stored radio stations
     * @param imagesSaveDir Destination path to save images
     * @param onLoad Every time when a station has been found and validated
     * @param onSave Every time when a station is going to be saved to database
     */
    suspend fun sync(
        imagesSaveDir: String,
        onLoad: ((station: BaseRadioInfo) -> Unit)? = null,
        onSave: ((station: RadioStation) -> Unit)? = null
    ) {
        stationsDao.deleteAll()
        getAllAvailableBaseStations(true) {
            GlobalScope.launch(context = Dispatchers.IO) {
                onLoad?.invoke(it)
                val station = fillRadioStation(it.id) ?: return@launch
                val dbStation = stationsDao.findByStationId(it.id)
                if (dbStation != null) {
                    station.id = dbStation.id
                    station.isFavorite = dbStation.isFavorite
                }

                val imagePath = downloadStationImage(it.id, imagesSaveDir)
                if (imagePath != null) {
                    station.imageUrl = imagePath
                }

                onSave?.invoke(station)
                stationsDao.insert(station)
            }
        }
    }

    /**
     * Get list of all available stations and filter it
     * with accessibility and extracting urls in case when
     * given url is a file or redirects to another one
     * @param onlyRunning Search only running stations
     * @return List of extracted filtered stations
     */
    public suspend fun getAllAvailableBaseStations(
        onlyRunning: Boolean = false,
        onNext: (station: BaseRadioInfo) -> Unit
    ): List<BaseRadioInfo> {
        // get list of all stations from API
        val allAPIStations = ArrayList<BaseRadioInfo>()
        var pageIndex = 0
        while (true) {
            val pageStations = api.getPage(pageIndex++)
            if (pageStations.isEmpty()) break
            allAPIStations.addAll(pageStations)
        }
        Log.d("SYNC", "Found ${allAPIStations.size} stations")
        // filter received stations
        val availableStations = arrayListOf<BaseRadioInfo>()
        allAPIStations.forEach { station ->
            Log.d("SYNC", "Fetching station#${station.id} ${station.title}")
            // Filter links
            val availableLinks = arrayListOf<Link>()
            station.links.forEach { link ->
                URLConverterFactory.extract(link.url, onlyRunning, urlConverterFactories)
                    ?.distinct()
                    ?.sortedBy { URLUtil.isHttpsUrl(it) }
                    ?.minBy { URLConverterFactory.isRunning(it) }
                    ?.let { availableLinks.add(Link(it, link.bitrate, link.app)) }
            }
            // Add station to result array
            if (availableLinks.isNotEmpty()) {
                station.links = availableLinks
                availableStations.add(station)
                onNext(station)
            }
        }

        return availableStations
    }

    /**
     * Get full radio station data
     */
    public suspend fun fillRadioStation(stationId: String): RadioStation? {
        return RadioStationFactory.from(api.getRadio(stationId) ?: return null)
    }

    /**
     * Download station image
     * @param stationId Id get download image
     * @param destPath Destination path to save image
     * @return Downloaded image path
     */
    public suspend fun downloadStationImage(
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
        val stationImage = api.getStationImage2(stationId)
        // validate received content type
        if (stationImage.contentType() == null || stationImage.contentType()!!.type() != "image") {
            return null
        }
        // save image on disk
        var filename: String? = "$destPath/$stationId.${stationImage.contentType()!!.subtype()}"
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
     * Delete all stations that are not in the array
     * and import array into database
     */
    public suspend fun syncDatabaseWith(stations: List<RadioStation>, force: Boolean = false) {
        if (force) {
            stationsDao.deleteAll()
        } else {
            val deleteRows = stationsDao.selectAll()
                .filter { db -> stations.any { it.stationId == db.stationId } }
            deleteRows.forEach { stationsDao.delete(it.stationId) }
        }
        stationsDao.insertAll(stations.map {
            stationsDao.findByStationId(it.stationId)?.let { db ->
                it.id = db.id
                it.isFavorite = db.isFavorite
            }
            it
        })
    }

    companion object {
        private var mInstance: RadioStationRepository? = null

        fun create(
            applicationContext: Context,
            stationsDao: RadioStationsDao,
            api: RadioTochkaAPI
        ) {
            mInstance = RadioStationRepository(stationsDao, api)
        }

        fun get(): RadioStationRepository {
            requireNotNull(mInstance)
            return mInstance!!
        }
    }
}
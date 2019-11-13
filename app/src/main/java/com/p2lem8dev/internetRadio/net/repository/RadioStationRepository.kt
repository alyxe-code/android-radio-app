package com.p2lem8dev.internetRadio.net.repository

import android.content.Context
import android.util.Log
import android.webkit.URLUtil
import com.p2lem8dev.internetRadio.database.radio.dao.RadioStationsDao
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.net.api.Link
import com.p2lem8dev.internetRadio.net.api.RadioBaseResponse
import com.p2lem8dev.internetRadio.net.api.RadioTochkaAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.net.HttpURLConnection
import java.net.URL

class RadioStationRepository(
    private val applicationContext: Context,
    private val stationsDao: RadioStationsDao,
    private val api: RadioTochkaAPI
) {

    suspend fun sync(imagesSaveDir: String) {
        val start = System.currentTimeMillis() / 1000
        val stations = ArrayList<RadioBaseResponse>()
        // get all available links
        getAllStationsShort()
            // validate links
            .filter { validateLinks(it.links).isNotEmpty() }
            // configure links
            .forEach { station ->
                val newLinks = arrayListOf<Link>()
                station.links.forEach setupURL@{ link ->
                    val ext = link.url.substring(link.url.lastIndexOf('.'))
                    // check if the url is a link to a remote file | .m4u
                    if (ext.substring(0, 4) == ".m4u") {
                        // get file text
                        val response = URL(link.url).readText()
                        if (!URLUtil.isValidUrl(response))
                            return@setupURL
                        // check response code
                        val url = URL(response).openConnection() as HttpURLConnection
                        if (url.responseCode != HttpURLConnection.HTTP_OK)
                            return@setupURL
                        // save changes
                        newLinks.add(
                            Link(
                                bitrate = link.bitrate,
                                app = link.bitrate,
                                url = response
                            )
                        )
                    }
                    // check if ext contains / or : -> a URL | and check it's not a file url
                    if ((ext.indexOf('/') != -1 || ext.indexOf(':') != -1) && !URLUtil.isFileUrl(
                            link.url
                        )
                    ) {
                        newLinks.add(link)
                    }
                }
                // update station data
                station.links = newLinks
                if (newLinks.size > 0) {
                    stations.add(station)
                }
            }

        Log.d("SYNC_TEST", "Configured ${stations.size} stations")

        // configure radio station and download image
        val radioStations = stations.mapNotNull { station ->
            var result: RadioStation? = null
            withContext(context = Dispatchers.IO) {
                getRadioStation(station.id)?.let { radioStation ->
                    radioStation.links = station.links.map { it.url } as ArrayList<String>
                    radioStation.imageUrl = downloadStationImage(station.id, imagesSaveDir)
                    result = radioStation
                }
            }
            result
        }

        Log.d("SYNC_TEST", "Filled ${stations.size} stations")
        Log.d("SYNC_TEST", "Synchronizing database")

        syncDatabaseWith(radioStations, force = true)

        Log.d("SYNC_TEST", "Database synchronized | ${stationsDao.selectAll().size}")
        val finish = System.currentTimeMillis() / 1000
        Log.d("SYNC_TEST", "Finished in ${finish - start} seconds")

        SessionRepository.get().updateSyncDate()
    }

    /**
     * Get list of all stations
     */
    private suspend fun getAllStationsShort(): List<RadioBaseResponse> {
        var index = 0
        val result = ArrayList<RadioBaseResponse>()
        while (true) {
            val items = api.getPage(index)
            if (items.isEmpty()) break
            result.addAll(items)
            index += items.size
        }

        return result
    }

    /**
     * Get full radio station data
     */
    private suspend fun getRadioStation(stationId: String): RadioStation? {
        val radio = api.getRadio(stationId) ?: return null
        return RadioStation(
            stationId = stationId,
            title = radio.title,
            country = radio.country,
            region = radio.region,
            city = radio.city,
            links = radio.links.map { it.url } as ArrayList<String>,
            listeners = radio.listeners.toInt(),
            views = radio.views.toInt(),
            playerStream = radio.playerStream,
            language = radio.language,
            genres = radio.genres.map { it.genre } as ArrayList<String>,
            voted = radio.voted.toInt(),
            isFavorite = false,
            imageUrl = null
        )
    }

    /**
     * Download station image
     */
    private suspend fun downloadStationImage(stationId: String, destPath: String): String? {
        val dir = File(destPath)
        require(dir.exists() && dir.isDirectory) { IllegalArgumentException("Destination path must be a directory") }

        val stationImage = api.getStationImage(stationId)

        requireNotNull(stationImage.contentType()) { IllegalArgumentException("Image content type must be defined") }
        require(stationImage.contentType()!!.type() == "image") { IllegalArgumentException("Image content type must be image") }

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
    private suspend fun syncDatabaseWith(stations: List<RadioStation>, force: Boolean = false) {
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

    private val supportedURLProtocols = listOf("http", "https")
    private val supportedContentTypes = listOf("audio/mpeg", "audio/x-mpegurl")

    private suspend fun validateLinks(links: List<Link>): List<Link> {
        return links.filter { link ->
            val protocol = link.url.split("://")[0]
            if (!supportedURLProtocols.contains(protocol)) return@filter false
            return@filter try {
                var result = false
                withContext(context = Dispatchers.IO) {
                    val connection = URL(link.url).openConnection()
                    if (connection.contentType == null) {
                        val ext = link.url.substring(link.url.lastIndexOf("."))
                        if (ext.substring(0, 4) == ".m3u") {
                            result = true
                            return@withContext
                        }
                    }
                    result = supportedContentTypes.contains(
                        connection.contentType
                    )
                }
                result
            } catch (e: Exception) {
                false
            }
        }
    }

    companion object {
        private var mInstance: RadioStationRepository? = null

        fun create(
            applicationContext: Context,
            stationsDao: RadioStationsDao,
            api: RadioTochkaAPI
        ) {
            mInstance = RadioStationRepository(applicationContext, stationsDao, api)
        }

        fun get(): RadioStationRepository {
            requireNotNull(mInstance)
            return mInstance!!
        }
    }
}
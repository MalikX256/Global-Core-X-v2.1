package com.example.data.map

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.local.AppDatabase
import com.example.data.local.entity.MapTileEntity
import com.example.data.local.entity.OfflineRegionEntity
import com.example.ui.components.map.MapStyle
import com.example.ui.components.map.MapUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val regionId: Long = 0,
    val regionName: String = "",
    val totalTiles: Int = 0,
    val downloadedTiles: Int = 0,
    val totalBytes: Long = 0L,
    val isFinished: Boolean = false,
    val errorMessage: String? = null
)

/**
 * High-performance Map Tile Manager responsible for:
 * 1. Rendering dynamic map tiles from online tile providers (CartoDB, OpenStreetMap, ArcGIS).
 * 2. Instant memory caching via LRU cache.
 * 3. Offline storage & caching using local Room Database (`map_tiles` and `offline_regions` tables).
 * 4. Background region downloader with progress monitoring.
 */
class MapTileManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val tileDao = db.mapTileDao()
    private val regionDao = db.offlineRegionDao()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Memory LRU Cache: Holds decoded ImageBitmaps in RAM for immediate 60fps canvas draw
    // Cache capacity: 300 tiles (~12MB RAM)
    private val memoryCache = LruCache<String, ImageBitmap>(300)

    // Tracks in-flight tile load requests to avoid duplicate fetches
    private val pendingRequests = ConcurrentHashMap<String, Job>()

    // Global listener for tile arrival to trigger Compose Canvas recomposition/repaint
    private val _tileUpdateSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val tileUpdateSignal: SharedFlow<Unit> = _tileUpdateSignal.asSharedFlow()

    // Offline Mode toggle
    private val _isOfflineOnly = MutableStateFlow(false)
    val isOfflineOnly: StateFlow<Boolean> = _isOfflineOnly.asStateFlow()

    // Active download progress state
    private val _activeDownload = MutableStateFlow<DownloadProgress?>(null)
    val activeDownload: StateFlow<DownloadProgress?> = _activeDownload.asStateFlow()

    private var activeDownloadJob: Job? = null

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun setOfflineMode(enabled: Boolean) {
        _isOfflineOnly.value = enabled
    }

    /**
     * Retrieves a tile ImageBitmap synchronously from memory cache if available.
     * If not in memory, triggers asynchronous background loading from Room Database or Network.
     */
    fun getTile(z: Int, x: Int, y: Int, style: MapStyle): ImageBitmap? {
        val tileKey = "$z/$x/$y"
        val cacheKey = "${style.name}:$tileKey"

        val cached = memoryCache.get(cacheKey)
        if (cached != null) {
            return cached
        }

        // Trigger asynchronous background load if not already pending
        if (!pendingRequests.containsKey(cacheKey)) {
            val job = scope.launch {
                try {
                    loadTileAsync(z, x, y, style, cacheKey, tileKey)
                } finally {
                    pendingRequests.remove(cacheKey)
                }
            }
            pendingRequests[cacheKey] = job
        }

        return null
    }

    private suspend fun loadTileAsync(
        z: Int,
        x: Int,
        y: Int,
        style: MapStyle,
        cacheKey: String,
        tileKey: String
    ) {
        // Step 1: Check Room Database Cache
        val dbTile = tileDao.getTile(tileKey, style.name)
        if (dbTile != null && dbTile.tileData.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeByteArray(dbTile.tileData, 0, dbTile.tileData.size)
            if (bitmap != null) {
                val imageBitmap = bitmap.asImageBitmap()
                memoryCache.put(cacheKey, imageBitmap)
                _tileUpdateSignal.tryEmit(Unit)
                return
            }
        }

        // Step 2: If offline only, do not hit network
        if (_isOfflineOnly.value) {
            return
        }

        // Step 3: Fetch from Tile Server over Network
        val urls = getTileUrls(z, x, y, style)
        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "GlobalCoreX-Android-App/1.0 (Location & Navigation; support@globalcorex.com)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyBytes = response.body?.bytes()
                        if (bodyBytes != null && bodyBytes.isNotEmpty()) {
                            val bitmap = BitmapFactory.decodeByteArray(bodyBytes, 0, bodyBytes.size)
                            if (bitmap != null) {
                                val imageBitmap = bitmap.asImageBitmap()
                                memoryCache.put(cacheKey, imageBitmap)

                                // Persist to Room Database in background
                                val entity = MapTileEntity(
                                    tileKey = tileKey,
                                    style = style.name,
                                    zoom = z,
                                    tileX = x,
                                    tileY = y,
                                    tileData = bodyBytes,
                                    sizeBytes = bodyBytes.size.toLong(),
                                    timestamp = System.currentTimeMillis()
                                )
                                tileDao.insertTile(entity)

                                _tileUpdateSignal.tryEmit(Unit)
                                return
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Try fallback url if any
            }
        }
    }

    private fun getTileUrls(z: Int, x: Int, y: Int, style: MapStyle): List<String> {
        return when (style) {
            MapStyle.CYBER_DARK -> listOf(
                "https://a.basemaps.cartocdn.com/rastertiles/dark_all/$z/$x/$y.png",
                "https://b.basemaps.cartocdn.com/rastertiles/dark_all/$z/$x/$y.png",
                "https://tile.openstreetmap.org/$z/$x/$y.png"
            )
            MapStyle.STREET -> listOf(
                "https://tile.openstreetmap.org/$z/$x/$y.png",
                "https://a.basemaps.cartocdn.com/rastertiles/voyager/$z/$x/$y.png",
                "https://b.tile.openstreetmap.org/$z/$x/$y.png"
            )
            MapStyle.SATELLITE -> listOf(
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x",
                "https://a.basemaps.cartocdn.com/rastertiles/voyager/$z/$x/$y.png",
                "https://tile.openstreetmap.org/$z/$x/$y.png"
            )
        }
    }

    /**
     * Downloads an offline map region into the Room database.
     */
    fun startOfflineRegionDownload(
        name: String,
        minLat: Double,
        minLng: Double,
        maxLat: Double,
        maxLng: Double,
        minZoom: Int = 11,
        maxZoom: Int = 15,
        style: MapStyle = MapStyle.STREET
    ) {
        cancelActiveDownload()

        activeDownloadJob = scope.launch {
            try {
                val tilesToFetch = MapUtils.calculateTilesForBounds(
                    minLat = minLat,
                    minLng = minLng,
                    maxLat = maxLat,
                    maxLng = maxLng,
                    minZoom = minZoom,
                    maxZoom = maxZoom
                )

                val regionEntity = OfflineRegionEntity(
                    name = name,
                    minLat = minLat,
                    minLng = minLng,
                    maxLat = maxLat,
                    maxLng = maxLng,
                    minZoom = minZoom,
                    maxZoom = maxZoom,
                    style = style.name,
                    totalTiles = tilesToFetch.size,
                    downloadedTiles = 0,
                    sizeBytes = 0L,
                    isCompleted = false
                )

                val regionId = regionDao.insertRegion(regionEntity)

                _activeDownload.value = DownloadProgress(
                    regionId = regionId,
                    regionName = name,
                    totalTiles = tilesToFetch.size,
                    downloadedTiles = 0,
                    totalBytes = 0L,
                    isFinished = false
                )

                var downloadedCount = 0
                var totalBytesAccumulator = 0L
                val semaphore = Semaphore(6) // 6 concurrent tile downloads

                val batchList = mutableListOf<MapTileEntity>()
                val lock = Any()

                tilesToFetch.map { (z, x, y) ->
                    async {
                        semaphore.withPermit {
                            if (!isActive) return@withPermit

                            val tileKey = "$z/$x/$y"
                            val urls = getTileUrls(z, x, y, style)

                            var tileData: ByteArray? = null
                            for (url in urls) {
                                try {
                                    val request = Request.Builder()
                                        .url(url)
                                        .header("User-Agent", "GlobalCoreX-OfflineDownloader/1.0 (support@globalcorex.com)")
                                        .build()

                                    httpClient.newCall(request).execute().use { resp ->
                                        if (resp.isSuccessful) {
                                            val bytes = resp.body?.bytes()
                                            if (bytes != null && bytes.isNotEmpty()) {
                                                tileData = bytes
                                                return@use
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    // continue to next url
                                }
                                if (tileData != null) break
                            }

                            if (tileData != null) {
                                val entity = MapTileEntity(
                                    tileKey = tileKey,
                                    style = style.name,
                                    zoom = z,
                                    tileX = x,
                                    tileY = y,
                                    tileData = tileData!!,
                                    regionId = regionId,
                                    sizeBytes = tileData!!.size.toLong(),
                                    timestamp = System.currentTimeMillis()
                                )

                                synchronized(lock) {
                                    downloadedCount++
                                    totalBytesAccumulator += tileData!!.size.toLong()
                                    batchList.add(entity)

                                    if (batchList.size >= 25) {
                                        val toInsert = ArrayList(batchList)
                                        batchList.clear()
                                        launch(Dispatchers.IO) {
                                            tileDao.insertTiles(toInsert)
                                        }
                                    }

                                    _activeDownload.value = DownloadProgress(
                                        regionId = regionId,
                                        regionName = name,
                                        totalTiles = tilesToFetch.size,
                                        downloadedTiles = downloadedCount,
                                        totalBytes = totalBytesAccumulator,
                                        isFinished = false
                                    )
                                }
                            }
                        }
                    }
                }.awaitAll()

                // Insert remaining batch
                if (batchList.isNotEmpty()) {
                    tileDao.insertTiles(batchList)
                }

                // Update completed status in Room database
                val updatedRegion = regionDao.getRegionById(regionId)?.copy(
                    downloadedTiles = downloadedCount,
                    sizeBytes = totalBytesAccumulator,
                    isCompleted = true
                )
                if (updatedRegion != null) {
                    regionDao.updateRegion(updatedRegion)
                }

                _activeDownload.value = DownloadProgress(
                    regionId = regionId,
                    regionName = name,
                    totalTiles = tilesToFetch.size,
                    downloadedTiles = downloadedCount,
                    totalBytes = totalBytesAccumulator,
                    isFinished = true
                )

                _tileUpdateSignal.tryEmit(Unit)

            } catch (e: CancellationException) {
                _activeDownload.value = null
            } catch (e: Exception) {
                Log.e("MapTileManager", "Error downloading region", e)
                _activeDownload.value = _activeDownload.value?.copy(
                    isFinished = true,
                    errorMessage = e.localizedMessage ?: "Download failed"
                )
            }
        }
    }

    fun cancelActiveDownload() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        _activeDownload.value = null
    }

    fun getAllOfflineRegions(): Flow<List<OfflineRegionEntity>> {
        return regionDao.getAllRegions()
    }

    fun getTotalCachedTileCount(): Flow<Int> {
        return tileDao.getCachedTilesCount()
    }

    fun getTotalCachedBytes(): Flow<Long> {
        return tileDao.getTotalCachedBytes()
    }

    suspend fun deleteOfflineRegion(regionId: Long) = withContext(Dispatchers.IO) {
        tileDao.deleteTilesForRegion(regionId)
        regionDao.deleteRegionById(regionId)
        memoryCache.evictAll()
        _tileUpdateSignal.tryEmit(Unit)
    }

    suspend fun clearAllTileCache() = withContext(Dispatchers.IO) {
        tileDao.clearAllTiles()
        memoryCache.evictAll()
        _tileUpdateSignal.tryEmit(Unit)
    }

    companion object {
        @Volatile
        private var INSTANCE: MapTileManager? = null

        fun getInstance(context: Context): MapTileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MapTileManager(context).also { INSTANCE = it }
            }
        }
    }
}

package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.LocationRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocationRepositoryTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveLocationRecord() = runBlocking {
        val locationDao = db.locationDao()
        val record = LocationRecordEntity(
            tripId = 1001L,
            latitude = 0.3476,
            longitude = 32.5825,
            accuracy = 4.5f,
            speed = 12.5f, // ~45 km/h
            heading = 180.0f,
            altitude = 1190.0,
            timestamp = System.currentTimeMillis()
        )

        val id = locationDao.insertLocation(record)
        assertNotNull(id)

        val recent = locationDao.getRecentLocations().first()
        assertEquals(1, recent.size)
        assertEquals(0.3476, recent[0].latitude, 0.0001)
        assertEquals(32.5825, recent[0].longitude, 0.0001)
        assertEquals(1190.0, recent[0].altitude, 0.1)
        assertEquals(12.5f, recent[0].speed, 0.01f)
    }
}

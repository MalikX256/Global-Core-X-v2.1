package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        DeviceEntity::class,
        LocationRecordEntity::class,
        TripEntity::class,
        RecordedRouteEntity::class,
        RouteWaypointEntity::class,
        SavedRouteEntity::class,
        EmergencyContactEntity::class,
        SosEventEntity::class,
        SharedSessionEntity::class,
        UserPreferencesEntity::class,
        MapTileEntity::class,
        OfflineRegionEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun deviceDao(): DeviceDao
    abstract fun locationDao(): LocationDao
    abstract fun tripDao(): TripDao
    abstract fun recordedRouteDao(): RecordedRouteDao
    abstract fun routeWaypointDao(): RouteWaypointDao
    abstract fun savedRouteDao(): SavedRouteDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun sosEventDao(): SosEventDao
    abstract fun sharedSessionDao(): SharedSessionDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun mapTileDao(): MapTileDao
    abstract fun offlineRegionDao(): OfflineRegionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "globalcorex_db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

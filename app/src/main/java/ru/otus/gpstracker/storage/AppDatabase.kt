package ru.otus.gpstracker.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.otus.gpstracker.storage.dao.LocationDao
import ru.otus.gpstracker.storage.entity.LocationEntity

@Database(version = 1, exportSchema = false, entities = [LocationEntity::class])
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}

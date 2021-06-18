package ru.otus.gpstracker

import android.app.Application
import androidx.room.Room
import ru.otus.gpstracker.storage.AppDatabase
import ru.otus.gpstracker.utils.ViewModelFactory

class App : Application() {

    lateinit var factory: ViewModelFactory
    lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        database = Room
//            .databaseBuilder(this, AppDatabase::class.java, "database")
            .inMemoryDatabaseBuilder(this, AppDatabase::class.java)
            .build()
        factory = ViewModelFactory(database)
    }
}
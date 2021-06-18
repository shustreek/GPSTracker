package ru.otus.gpstracker.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.otus.gpstracker.App
import ru.otus.gpstracker.storage.AppDatabase
import ru.otus.gpstracker.ui.main.MainViewModel

class ViewModelFactory(private val appDatabase: AppDatabase) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when (modelClass) {
            MainViewModel::class.java -> MainViewModel(appDatabase.locationDao())
            else -> throw IllegalArgumentException("Cannot find $modelClass")
        } as T
    }
}

inline val Fragment.factory: ViewModelFactory
    get() = (requireActivity().application as App).factory
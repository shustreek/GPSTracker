package ru.otus.gpstracker.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ru.otus.gpstracker.utils.SingleLiveEvent

class MainViewModel : ViewModel() {

    private var isMapReady: Boolean = false
    private var isPermissionGranded: Boolean = false

    private val mShowDialog: MutableLiveData<Unit> = SingleLiveEvent<Unit>()
    private val mStartLocation: MutableLiveData<Unit> = SingleLiveEvent<Unit>()
    private val mShowMyLocation = MutableLiveData<Boolean>()


    val showDialog: LiveData<Unit> = mShowDialog
    val startLocation: LiveData<Unit> = mStartLocation
    val showMyLocation: LiveData<Boolean> = mShowMyLocation

    fun onRequestResult(granded: Boolean) {
        isPermissionGranded = granded
        if (granded) {
            if (isMapReady) mShowMyLocation.value = true
            mStartLocation.value = Unit
        } else {
            mShowDialog.value = Unit
        }
    }

    fun onMapReady() {
        isMapReady = true
        mShowMyLocation.value = isPermissionGranded
    }

}
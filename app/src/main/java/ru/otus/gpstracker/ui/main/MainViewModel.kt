package ru.otus.gpstracker.ui.main

import android.location.Location
import androidx.lifecycle.*
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import ru.otus.gpstracker.storage.dao.LocationDao
import ru.otus.gpstracker.storage.entity.LocationEntity
import ru.otus.gpstracker.utils.SingleLiveEvent

class MainViewModel(private val locationDao: LocationDao) : ViewModel() {

    private var isMapReady: Boolean = false
    private var isPermissionGranded: Boolean = false
    private val pointsList = mutableListOf<LatLng>()

    private val mShowDialog: MutableLiveData<Unit> = SingleLiveEvent<Unit>()
    private val mStartLocation: MutableLiveData<Boolean> = SingleLiveEvent<Boolean>()
    private val mShowMyLocation = MutableLiveData<Boolean>()
    private val mPoints = MutableLiveData<List<LatLng>>()
    private val mLocation: MediatorLiveData<Location> = MediatorLiveData()


    val showDialog: LiveData<Unit> = mShowDialog
    val startLocation: LiveData<Boolean> = mStartLocation
    val showMyLocation: LiveData<Boolean> = mShowMyLocation
    val points: LiveData<List<LatLng>> = mPoints
    val location: LiveData<Location> = mLocation

    fun onRequestResult(granded: Boolean) {
        isPermissionGranded = granded
        if (granded) {
            if (isMapReady) mShowMyLocation.value = true
            mStartLocation.value = true
        } else {
            mShowDialog.value = Unit
        }
    }

    fun onMapReady() {
        isMapReady = true
        mShowMyLocation.value = isPermissionGranded

        viewModelScope.launch {
            val list = locationDao.getLocations()
                .map { LatLng(it.lat, it.lng) }
            pointsList.clear()
            pointsList.addAll(list)
            mPoints.value = list

            mLocation.addSource(locationDao.getLastLocation(), Observer {
                if (it == null) {
                    mPoints.value = pointsList
                    return@Observer
                }
                mLocation.value = it.mapToLocation()
                pointsList.add(LatLng(it.lat, it.lng))
                mPoints.value = pointsList
            })

        }
    }

    fun onLocationResult(result: LocationResult) {
        val lastLocation = result.lastLocation
        pointsList.add(LatLng(lastLocation.latitude, lastLocation.longitude))
        mPoints.value = pointsList
    }

    fun onStopClick() {
        mStartLocation.value = false
    }

}

private fun LocationEntity.mapToLocation(): Location {
    return Location(provider).also {
        it.time = timestamp
        it.latitude = lat
        it.longitude = lng
        it.speed = speed
        it.bearing = bearing
        it.altitude = altitude
        it.accuracy = accuracy
    }
}

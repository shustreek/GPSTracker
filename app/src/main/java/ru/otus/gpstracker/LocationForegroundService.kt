package ru.otus.gpstracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import ru.otus.gpstracker.storage.dao.LocationDao
import ru.otus.gpstracker.storage.entity.LocationEntity

class LocationForegroundService : LifecycleService() {

    private val locationDao: LocationDao by lazy {
        (application as App).database.locationDao()
    }

    private val manager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationRequest = LocationRequest()
        .setInterval(1_000)
        .setFastestInterval(1_000)
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

    private val locationCallback: LocationCallback = object : LocationCallback() {

        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            if (result == null) return
            onNewLocation(result.lastLocation)
        }
    }

    private fun onNewLocation(location: Location) {
        lifecycleScope.launch {
            locationDao.insert(location.mapToEntity())
        }
    }

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        val permission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission == PackageManager.PERMISSION_GRANTED) {
            startLocationService()
            startLocation()
        } else {
            stopForeground(true)
        }
    }

    private fun startLocationService() {
        createChanel()
        createNotification()
    }

    private fun createNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setOngoing(true)
            .setSilent(true)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_message))
            .setCategory(Notification.CATEGORY_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_location)
            .setColor(ContextCompat.getColor(this, R.color.teal_700))
            .build()

        when {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q -> {
                startForeground(
                    NOTIFY_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            }
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O -> {
                startForeground(
                    NOTIFY_ID,
                    notification
                )
            }
            else -> {
                manager.notify(NOTIFY_ID, notification)
            }
        }
    }

    private fun createChanel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocation() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    companion object {
        private const val CHANNEL_ID = "location_channel"
        private const val NOTIFY_ID = 42
    }
}

private fun Location.mapToEntity(): LocationEntity {
    return LocationEntity(
        timestamp = time,
        lat = latitude,
        lng = longitude,
        accuracy = accuracy,
        altitude = altitude,
        bearing = bearing,
        provider = provider,
        speed = speed
    )
}

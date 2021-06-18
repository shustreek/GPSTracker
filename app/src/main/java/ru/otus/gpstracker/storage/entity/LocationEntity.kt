package ru.otus.gpstracker.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "location")
data class LocationEntity(

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "lat")
    val lat: Double,

    @ColumnInfo(name = "lng")
    val lng: Double,

    @ColumnInfo(name = "accuracy")
    val accuracy: Float,

    @ColumnInfo(name = "altitude")
    val altitude: Double,

    @ColumnInfo(name = "bearing")
    val bearing: Float,

    @ColumnInfo(name = "provider")
    val provider: String?,

    @ColumnInfo(name = "speed")
    val speed: Float,

    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0
)
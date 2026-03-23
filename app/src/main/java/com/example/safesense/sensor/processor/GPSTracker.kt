package com.example.safesense.sensor.processor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

class GPSTracker(
    private val context: Context
) : LocationListener {

    companion object {
        const val UPDATE_INTERVAL_MS = 30_000L  // 30 seconds during normal monitoring
        const val MIN_DISTANCE_METERS = 10f     // minimum 10m movement before update
    }

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Last known fix always kept in memory — never null after first fix
    @Volatile
    private var lastKnownLocation: Location? = null

    val lastLocation: Location? get() = lastKnownLocation

    @SuppressLint("MissingPermission")
    fun start() {
        // forceAndroidLocationManager — non-negotiable from build guide
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            UPDATE_INTERVAL_MS,
            MIN_DISTANCE_METERS,
            this
        )

        // Seed with last known fix immediately so first SMS can go out instantly
        val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        lastKnownLocation = when {
            lastGps != null && lastNetwork != null -> {
                if (lastGps.time > lastNetwork.time) lastGps else lastNetwork
            }
            lastGps != null -> lastGps
            lastNetwork != null -> lastNetwork
            else -> null
        }
    }

    fun stop() {
        locationManager.removeUpdates(this)
    }

    // Returns GPS coordinates formatted for SMS Google Maps link
    // Returns null if no fix available yet
    fun getLocationForSms(): String? {
        val location = lastKnownLocation ?: return null
        return "https://maps.google.com/?q=${location.latitude},${location.longitude}"
    }

    override fun onLocationChanged(location: Location) {
        lastKnownLocation = location
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}
}
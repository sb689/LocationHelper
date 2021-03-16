package com.example.locationhelper


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.locationhelper.network.ApiResponse
import com.example.locationhelper.network.NetworkHelper
import com.google.android.gms.location.*
import kotlinx.coroutines.*

import retrofit2.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.Exception


data class LocationEvent(var lat: Float, var lon: Float, var time: Long, val ext: String)



class Library(baseUrl: String, contextParam: Context ) {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location? = null
    private val context: Context = contextParam
    var endPoint: String = baseUrl
    var fineLocationPermissionGranted = false
    var coarseLocationPermissionGranted = false



    fun setup(): Boolean {
        val locationSettings =  setupLocationSettings()
        if(locationSettings)
            requestToLocationUpdates()
        return locationSettings

    }

    fun cleanLogger(){
        removeLocationUpdates()
    }

    suspend fun log(event: LocationEvent): Boolean {
        // POST to API Server

            try {

                val apiService = NetworkHelper.getRetrofitInstance(endPoint)
                val response: Response<ApiResponse> = apiService.saveLocation(validateLocation(event))
                if (response.code() != 200) {
                    Log.e(TAG, "an error occurred while posting location to the server: $response")
                    throw IOException("server error code ${response.code()}")
                }else{
                    Log.d(TAG, "location post to server is successful")
                }
                return true
            }catch(ex: Exception)
            {
                try {
                    val apiService = NetworkHelper.getRetrofitInstance(endPoint)
                    apiService.logErrorMessages(ex.message.toString())
                }
                catch (ex:Exception) {
                    Log.e(TAG, "error occurred while posting location to the server: ${ex.message}")
                }
                return false
            }

    }



    private fun validateLocation(event:LocationEvent) : LocationEvent
    {
        var location: LocationEvent = event
        if(event.lat in -90.0..90.0)
        {
            location.lat = event.lat
        }else{
            location.lat = currentLocation?.latitude?.toFloat() ?: 0F
        }
        if(event.lon in -180.0..180.0){
            location.lon = event.lon
        }
        else{
            location.lon = currentLocation?.longitude?.toFloat() ?: 0F
        }
        if(event.time <= 0) {
            location.time =  System.currentTimeMillis()
        }
        return location
    }

    private fun setupLocationSettings(): Boolean{


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        checkLocationPermission()
        Log.d(TAG, "locationPermissionChecked, fineLocationPermissionGranted = "
                + fineLocationPermissionGranted +
                ", coarseLocationPermissionGranted = " + coarseLocationPermissionGranted)

        locationRequest = LocationRequest.create().apply {

            interval = TimeUnit.SECONDS.toMillis(30)
            fastestInterval = TimeUnit.SECONDS.toMillis(30)
            maxWaitTime = TimeUnit.MINUTES.toMillis(2)

            if(fineLocationPermissionGranted)
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            else if (coarseLocationPermissionGranted)
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }

        return if(fineLocationPermissionGranted || coarseLocationPermissionGranted) {

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    currentLocation = locationResult.lastLocation
                    Log.d(TAG, "updated location = " + currentLocation.toString())
                }
            }
            true
        }
        else
            false
    }

    fun requestToLocationUpdates() {

        try { fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper())
        } catch (ex: SecurityException) {
            Log.e(TAG, "error while subscribing to location update")
        }
    }

    fun removeLocationUpdates() {

        try {
            val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            removeTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Location Callback removed.")
                } else {
                    Log.d(TAG, "Failed to remove Location Callback.")
                }
            }

        } catch (unlikely: SecurityException) {
            Log.e(TAG, "error while removing location update")
        }
    }

    private fun checkLocationPermission(){
        fineLocationPermissionGranted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        coarseLocationPermissionGranted = ActivityCompat.checkSelfPermission(
               context,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    companion object {
        private const val TAG = "Library"
    }




}
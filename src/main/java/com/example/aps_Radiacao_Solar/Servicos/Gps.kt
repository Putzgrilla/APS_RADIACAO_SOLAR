package com.example.aps_Radiacao_Solar.Servicos

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class Gps(private val context: Context) {
    private var callback: ((Double?, Double?, String?) -> Unit)? = null


    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    fun solicitarPermissoes(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    fun checkLocationPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun obterLocalizacao(
        context: Context,
        callback: (latitude: Double?, longitude: Double?, erro: String?) -> Unit
    ) {
        this.callback = callback

        if (!checkLocationPermissions(context)) {
            callback(null, null, "“Sem permissões necessárias para continuar ")
            return
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    Log.e("gpslog", " latiude:$latitude  longitude:$longitude")
                    callback(latitude, longitude, null)
                } else {
                    callback(null, null, "Não foi possivel conseguir sua localização")
                }
            }

        }
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    context?.let { ctx ->
                        callback?.let { cb ->
                            obterLocalizacao(ctx, cb)
                        }
                    }
                } else {
                    callback?.invoke(null, null, "“permissões necessárias negadas")
                }
            }
        }
    }


}
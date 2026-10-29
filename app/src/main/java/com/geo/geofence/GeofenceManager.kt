package com.geo.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.geo.screen.Group
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GeofenceManager"

@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = LocationServices.getGeofencingClient(context)


    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }


    private val mutex = Mutex()


    @SuppressLint("MissingPermission") // checked by hasGeofencePermission()
    suspend fun sync(groups: List<Group>) = mutex.withLock {
        if (!context.hasGeofencePermission()) return@withLock

        val fences = groups.mapNotNull { group ->
            val fence = group.fence ?: return@mapNotNull null
            Geofence.Builder()
                .setRequestId(group.groupId) // tells the receiver which group was exited
                .setCircularRegion(fence.lat, fence.lng, fence.radiusM.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }

        runCatching {
            client.removeGeofences(pendingIntent).await()
            if (fences.isNotEmpty()) {
                val request = GeofencingRequest.Builder()

                    .setInitialTrigger(0)
                    .addGeofences(fences)
                    .build()
                client.addGeofences(request, pendingIntent).await()
            }
            Log.d(TAG, "Monitoring ${fences.size} geofence(s)")
        }.onFailure { Log.e(TAG, "Geofence sync failed", it) }
    }
}


fun Context.hasGeofencePermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return fine
    return fine && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
}

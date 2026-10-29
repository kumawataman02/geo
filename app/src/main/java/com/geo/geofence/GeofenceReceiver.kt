package com.geo.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.geo.screen.MainRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "GeofenceReceiver"


@EntryPoint
@InstallIn(SingletonComponent::class)
interface GeofenceReceiverEntryPoint {
    fun mainRepository(): MainRepository
}


class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e(TAG, GeofenceStatusCodes.getStatusCodeString(event.errorCode))
            return
        }
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT) return

        val groupIds = event.triggeringGeofences.orEmpty().map { it.requestId }
        Log.d(TAG, "EXIT from groups: $groupIds")

        val repository = EntryPointAccessors
            .fromApplication(context, GeofenceReceiverEntryPoint::class.java)
            .mainRepository()


        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(8_000.milliseconds) {
                    groupIds.forEach { groupId ->
                        repository.addExitEvent(groupId)
                            .onFailure { Log.e(TAG, "Failed to save exit for $groupId", it) }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

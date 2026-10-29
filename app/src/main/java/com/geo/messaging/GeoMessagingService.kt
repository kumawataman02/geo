package com.geo.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.geo.R
import com.geo.auth.LoginRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "GeoMessagingService"
private const val CHANNEL_ID = "geofence_alerts"

@AndroidEntryPoint
class GeoMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var loginRepository: LoginRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // FCM rotates tokens (reinstall, data cleared): keep Firestore up to date
    override fun onNewToken(token: String) {
        scope.launch {
            runCatching { loginRepository.saveFcmToken(token) }
                .onFailure { Log.e(TAG, "Failed to save FCM token", it) }
        }
    }

    // Data-only messages always land here, app open or closed
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["type"] != "EXIT") return
        showExitNotification(
            name = data["name"].orEmpty(),
            groupName = data["groupName"].orEmpty(),
            // Same member + same group replaces the old alert instead of stacking
            id = "${data["groupId"]}_${data["uid"]}".hashCode()
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

private fun Context.showExitNotification(name: String, groupName: String, id: Int) {
    val manager = getSystemService(NotificationManager::class.java)
    // Android 13+: user denied POST_NOTIFICATIONS, nothing to show
    if (!manager.areNotificationsEnabled()) return

    // Safe to call every time; Android ignores it if the channel exists
    manager.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, "Geofence alerts", NotificationManager.IMPORTANCE_HIGH)
    )

    val openApp = PendingIntent.getActivity(
        this,
        0,
        packageManager.getLaunchIntentForPackage(packageName),
        PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("$name left the geofence")
        .setContentText("$name is outside $groupName's area")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(openApp)
        .setAutoCancel(true)
        .build()

    manager.notify(id, notification)
}

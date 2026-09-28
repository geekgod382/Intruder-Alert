package com.example.intruder_alert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat

class ScreenMonitorService : Service() {
    private val CHANNELID = "prank_service_channel"

    companion object {
        var isRunning = false
            private set // Prevents external classes from changing it directly
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                val prankIntent = Intent(context, PrankActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                context.startActivity(prankIntent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNELID)
            .setContentTitle("Intruder Alert Active")
            .setContentText("Monitoring for unauthorized access...")
            .setSmallIcon(android.R.drawable.ic_lock_lock) // Use a system icon for now
            .build()

        // ID must not be 0
        startForeground(1, notification)

        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        // Note: For Android 14+, you might need to specify RECEIVER_EXPORTED or NOT_EXPORTED
        registerReceiver(screenReceiver, filter)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNELID,
                "Intruder Alert Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        isRunning = false
        unregisterReceiver(screenReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}

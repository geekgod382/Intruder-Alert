package com.example.intruder_alert

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale

class NotificationService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var isTtsInitialized = false

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            isTtsInitialized = true
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isTtsInitialized) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: "Someone"
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        val announcement = when {
            // WhatsApp
            packageName.contains("com.whatsapp") ->
                "One message from $title"

            // Gmail
            packageName.contains("com.google.android.gm") ->
                "A new mail by $title"

            // Calendar
            packageName.contains("com.google.android.calendar") ->
                "Calendar reminder: $text"

            // Text Messages (SMS)
            packageName.contains("com.google.android.apps.messaging") ->
                "New text from $title"

            else -> null
        }

        announcement?.let {
            tts.speak(it, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
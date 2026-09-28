package com.example.intruder_alert

import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.biometric.BiometricPrompt

class PrankActivity : androidx.fragment.app.FragmentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var biometricPrompt: BiometricPrompt
    private var savedName : String = "owner"

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("PrankPrefs", android.content.Context.MODE_PRIVATE)
        savedName = prefs.getString("user_name", "owner") ?: "owner"

        // Ensure the activity shows over the lock screen
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        tts = TextToSpeech(this, this)
        setupBiometricPrompt()
    }

    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        // List of funny/scary messages
        val failureMessages = listOf(
            "Identify yourself, stranger! Put the phone down!",
            "I know you are not $savedName! I am calling the police!",
            "Access denied! Intruders will be vaporized in five seconds.",
            "Stop touching me! You are not my owner!",
            "Alert! Alert! Unknown human detected!",
            "Get lost, dipshit!"
        )

        biometricPrompt = BiometricPrompt(this, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                speak("Welcome $savedName!")
                // Use a Handler to wait 2 seconds before closing, so TTS can finish
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    finish()
                }, 2000)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Pick a random message from the list
                //speak(failureMessages.random())

                // Don't finish immediately on error so the prank stays on screen
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    finish()
                }, 5000)
            }

            override fun onAuthenticationFailed() {
                // This triggers on every "bad" finger tap
                speak(failureMessages.random())
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Identity Verification")
            .setSubtitle("Place your finger to proceed")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }


    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = java.util.Locale.US
        }
    }
    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
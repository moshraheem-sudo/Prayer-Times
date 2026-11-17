package com.example.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver triggered when the user dismisses / swipes away the Adhan playing notification
 * (swiping left or right) or taps the Stop action button.
 * Immediately terminates Adhan audio playback, releases wake locks and stops the foreground service.
 */
class AdhanStopReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("AdhanStopReceiver", "Received stop/dismiss action: ${intent?.action}")
        try {
            // 1. Direct synchronous stop on running instance (instant, no IPC delay)
            AdhanAudioService.stopCurrentPlayback()
            // 2. Service action fallback to guarantee full cleanup
            AdhanAudioService.stopAdhan(context)
        } catch (e: Exception) {
            Log.e("AdhanStopReceiver", "Error stopping Adhan playback: ${e.message}")
        }
    }
}

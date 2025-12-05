package com.roos.easywakeword

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver that listens for Hotword Plugin events.
 * 
 * Hotword Plugin uses the Tasker/Locale plugin API to fire events.
 * When a hotword is detected, this receiver is triggered and launches
 * Home Assistant Assist.
 */
class HotwordEventReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HotwordEventReceiver"
        const val HOME_ASSISTANT_PACKAGE = "io.homeassistant.companion.android"
        
        // Locale/Tasker plugin actions
        const val ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
        const val ACTION_QUERY_CONDITION = "com.twofortyfouram.locale.intent.action.QUERY_CONDITION"
        
        // Hotword Plugin specific action (if it broadcasts directly)
        const val ACTION_HOTWORD_DETECTED = "nl.jolanrensen.hotwordPlugin.HOTWORD_DETECTED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")
        
        when (intent.action) {
            ACTION_FIRE_SETTING,
            ACTION_QUERY_CONDITION,
            ACTION_HOTWORD_DETECTED -> {
                Log.i(TAG, "Hotword detected! Launching Home Assistant Assist")
                launchHomeAssistantAssist(context)
            }
            else -> {
                Log.d(TAG, "Unknown action: ${intent.action}")
            }
        }
    }

    private fun launchHomeAssistantAssist(context: Context) {
        try {
            // Try to launch Home Assistant Assist directly
            val assistIntent = Intent().apply {
                action = "android.intent.action.ASSIST"
                setPackage(HOME_ASSISTANT_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (assistIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(assistIntent)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch via ASSIST action", e)
        }

        try {
            // Try the Home Assistant deep link for Assist
            val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("homeassistant://navigate/assist")
                setPackage(HOME_ASSISTANT_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (deepLinkIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(deepLinkIntent)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch via deep link", e)
        }

        try {
            // Fallback: Launch the main Home Assistant app
            val launchIntent = context.packageManager.getLaunchIntentForPackage(HOME_ASSISTANT_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch Home Assistant app", e)
        }

        Log.e(TAG, "Could not launch Home Assistant")
    }
}

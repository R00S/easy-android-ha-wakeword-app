package com.roos.easywakeword

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Foreground service that keeps the app alive to receive hotword events.
 * 
 * This service registers a dynamic broadcast receiver to listen for
 * Hotword Plugin events and launches Home Assistant Assist when triggered.
 */
class HotwordListenerService : Service() {

    companion object {
        private const val TAG = "HotwordListenerService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "hotword_listener_channel"
        
        fun start(context: Context) {
            val intent = Intent(context, HotwordListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, HotwordListenerService::class.java))
        }
        
        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (HotwordListenerService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    private var hotwordReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        registerHotwordReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        unregisterHotwordReceiver()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SetupWizardActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun registerHotwordReceiver() {
        hotwordReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "Hotword event received: ${intent.action}")
                launchHomeAssistantAssist()
            }
        }

        val filter = IntentFilter().apply {
            addAction(HotwordEventReceiver.ACTION_FIRE_SETTING)
            addAction(HotwordEventReceiver.ACTION_QUERY_CONDITION)
            addAction(HotwordEventReceiver.ACTION_HOTWORD_DETECTED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(hotwordReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(hotwordReceiver, filter)
        }
        
        Log.d(TAG, "Hotword receiver registered")
    }

    private fun unregisterHotwordReceiver() {
        hotwordReceiver?.let {
            try {
                unregisterReceiver(it)
                Log.d(TAG, "Hotword receiver unregistered")
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering receiver", e)
            }
        }
        hotwordReceiver = null
    }

    private fun launchHomeAssistantAssist() {
        try {
            // Try to launch Home Assistant Assist directly
            val assistIntent = Intent().apply {
                action = "android.intent.action.ASSIST"
                setPackage(HotwordEventReceiver.HOME_ASSISTANT_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (assistIntent.resolveActivity(packageManager) != null) {
                startActivity(assistIntent)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch via ASSIST action", e)
        }

        try {
            // Try the Home Assistant deep link for Assist
            val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("homeassistant://navigate/assist")
                setPackage(HotwordEventReceiver.HOME_ASSISTANT_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (deepLinkIntent.resolveActivity(packageManager) != null) {
                startActivity(deepLinkIntent)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch via deep link", e)
        }

        try {
            // Fallback: Launch the main Home Assistant app
            val launchIntent = packageManager.getLaunchIntentForPackage(HotwordEventReceiver.HOME_ASSISTANT_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch Home Assistant app", e)
        }

        Log.e(TAG, "Could not launch Home Assistant")
    }
}

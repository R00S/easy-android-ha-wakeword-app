package com.roos.easywakeword.wakeword

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.roos.easywakeword.R
import com.roos.easywakeword.SetupWizardActivity
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Foreground service that continuously listens for the wake word
 * and launches Home Assistant Assist when detected.
 */
class WakeWordService : Service() {
    
    companion object {
        private const val TAG = "WakeWordService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "wake_word_channel"
        
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_IN_SHORTS = 1280
        
        private const val DETECTION_THRESHOLD = 0.05f
        private const val HOME_ASSISTANT_PACKAGE = "io.homeassistant.companion.android"
        
        // Cooldown period to prevent rapid-fire launches (3 seconds)
        private const val LAUNCH_COOLDOWN_MS = 3000L
        
        // Broadcast action for audio level updates
        const val ACTION_AUDIO_LEVEL = "com.roos.easywakeword.AUDIO_LEVEL"
        const val EXTRA_AUDIO_LEVEL = "audio_level"
        const val EXTRA_PREDICTION_SCORE = "prediction_score"
        
        // Track service running state
        @Volatile
        private var serviceRunning = false
        
        fun start(context: Context) {
            val intent = Intent(context, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, WakeWordService::class.java))
        }
        
        fun isRunning(context: Context): Boolean {
            return serviceRunning
        }
    }
    
    private var modelRunner: OnnxModelRunner? = null
    private var wakeWordModel: WakeWordModel? = null
    private var audioRecorderThread: AudioRecorderThread? = null
    private var isListening = false
    private var lastLaunchTime = 0L
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        serviceRunning = true
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        startForeground(NOTIFICATION_ID, createNotification())
        startListening()
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        serviceRunning = false
        stopListening()
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
    
    private fun startListening() {
        if (isListening) return
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            stopSelf()
            return
        }
        
        try {
            modelRunner = OnnxModelRunner(assets)
            wakeWordModel = WakeWordModel(modelRunner!!)
            
            audioRecorderThread = AudioRecorderThread()
            audioRecorderThread?.start()
            isListening = true
            
            Log.d(TAG, "Wake word detection started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start wake word detection", e)
            stopSelf()
        }
    }
    
    private fun stopListening() {
        isListening = false
        audioRecorderThread?.stopRecording()
        audioRecorderThread = null
        
        modelRunner?.close()
        modelRunner = null
        wakeWordModel = null
        
        Log.d(TAG, "Wake word detection stopped")
    }
    
    private fun onWakeWordDetected() {
        // Check cooldown to prevent rapid-fire launches
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLaunchTime < LAUNCH_COOLDOWN_MS) {
            Log.d(TAG, "Wake word detected but within cooldown period, ignoring")
            return
        }
        
        Log.i(TAG, "Wake word detected! Launching Home Assistant Assist")
        lastLaunchTime = currentTime
        launchHomeAssistantAssist()
    }
    
    private fun launchHomeAssistantAssist() {
        try {
            // Method 1: Launch AssistActivity directly with explicit component
            val assistIntent = Intent(Intent.ACTION_ASSIST).apply {
                setClassName(HOME_ASSISTANT_PACKAGE, "$HOME_ASSISTANT_PACKAGE.assist.AssistActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            
            if (assistIntent.resolveActivity(packageManager) != null) {
                Log.d(TAG, "Launching Assist via explicit component")
                startActivity(assistIntent)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch via explicit AssistActivity", e)
        }
        
        try {
            // Method 2: Use VOICE_COMMAND action (alternative intent for Assist)
            val voiceCommandIntent = Intent("android.intent.action.VOICE_COMMAND").apply {
                setPackage(HOME_ASSISTANT_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (voiceCommandIntent.resolveActivity(packageManager) != null) {
                Log.d(TAG, "Launching Assist via VOICE_COMMAND")
                startActivity(voiceCommandIntent)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch via VOICE_COMMAND action", e)
        }
        
        try {
            // Method 3: Use generic ASSIST action with package filter
            val genericAssistIntent = Intent(Intent.ACTION_ASSIST).apply {
                setPackage(HOME_ASSISTANT_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (genericAssistIntent.resolveActivity(packageManager) != null) {
                Log.d(TAG, "Launching Assist via generic ASSIST action")
                startActivity(genericAssistIntent)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch via ASSIST action", e)
        }
        
        try {
            // Method 4: Try the Home Assistant deep link for Assist
            val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("homeassistant://navigate/assist")
                setPackage(HOME_ASSISTANT_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (deepLinkIntent.resolveActivity(packageManager) != null) {
                Log.d(TAG, "Launching Assist via deep link")
                startActivity(deepLinkIntent)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch via deep link", e)
        }
        
        try {
            // Fallback: Launch the main Home Assistant app
            val launchIntent = packageManager.getLaunchIntentForPackage(HOME_ASSISTANT_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                Log.d(TAG, "Launching Home Assistant main app as fallback")
                startActivity(launchIntent)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch Home Assistant app", e)
        }
        
        Log.e(TAG, "Could not launch Home Assistant - app may not be installed")
    }
    
    private fun broadcastAudioLevel(audioLevel: Float, predictionScore: Float) {
        val intent = Intent(ACTION_AUDIO_LEVEL).apply {
            putExtra(EXTRA_AUDIO_LEVEL, audioLevel)
            putExtra(EXTRA_PREDICTION_SCORE, predictionScore)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    
    private inner class AudioRecorderThread : Thread() {
        private var audioRecord: AudioRecord? = null
        @Volatile
        private var recording = true
        private var lastBroadcastTime = 0L
        private val broadcastIntervalMs = 100L // Limit broadcasts to 10 per second
        
        @Suppress("MissingPermission")
        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = maxOf(minBufferSize, BUFFER_SIZE_IN_SHORTS * 2)
            
            // Use VOICE_RECOGNITION audio source for better far-field audio capture
            // This typically uses the best available microphone for voice input
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                return
            }
            
            val audioBuffer = ShortArray(BUFFER_SIZE_IN_SHORTS)
            audioRecord?.startRecording()
            
            Log.d(TAG, "Audio recording started with VOICE_RECOGNITION source")
            
            while (recording && isListening) {
                val readResult = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                
                if (readResult > 0) {
                    // Convert short array to float array (normalized to -1.0 to 1.0)
                    val floatBuffer = FloatArray(audioBuffer.size) { i ->
                        audioBuffer[i] / 32768.0f
                    }
                    
                    // Calculate audio level (RMS in dB)
                    val audioLevel = calculateAudioLevel(audioBuffer)
                    
                    // Predict wake word
                    val prediction = wakeWordModel?.predictWakeWord(floatBuffer) ?: 0f
                    
                    // Broadcast audio level for UI feedback (throttled)
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBroadcastTime >= broadcastIntervalMs) {
                        broadcastAudioLevel(audioLevel, prediction)
                        lastBroadcastTime = currentTime
                    }
                    
                    // Log all predictions above 0.01 for debugging
                    if (prediction > 0.01f) {
                        Log.d(TAG, "Wake word prediction: $prediction (threshold: $DETECTION_THRESHOLD)")
                    }
                    
                    if (prediction > DETECTION_THRESHOLD) {
                        Log.i(TAG, "Wake word DETECTED! Score: $prediction")
                        onWakeWordDetected()
                    }
                }
            }
            
            releaseResources()
        }
        
        private fun calculateAudioLevel(buffer: ShortArray): Float {
            // Calculate RMS (Root Mean Square) for audio level using Long to avoid overflow
            var sum = 0L
            for (sample in buffer) {
                sum += sample.toLong() * sample.toLong()
            }
            val rms = sqrt(sum.toDouble() / buffer.size)
            
            // Convert to dB scale (0-100 range for UI)
            // Max short value is 32768, so max RMS would be around that
            val db = if (rms > 0) 20 * log10(rms / 32768.0) else -100.0
            
            // Normalize to 0-100 scale (-60dB to 0dB range)
            val normalized = ((db + 60) / 60 * 100).coerceIn(0.0, 100.0)
            return normalized.toFloat()
        }
        
        fun stopRecording() {
            recording = false
        }
        
        private fun releaseResources() {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d(TAG, "Audio recording stopped")
        }
    }
}

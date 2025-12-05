package com.roos.easywakeword

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.roos.easywakeword.databinding.ActivitySetupWizardBinding
import com.roos.easywakeword.wakeword.WakeWordService

class SetupWizardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupWizardBinding
    private var currentStep = 1
    private val totalSteps = 3

    companion object {
        const val HOME_ASSISTANT_PACKAGE = "io.homeassistant.companion.android"
        
        // Delay for UI update after service state change
        private const val SERVICE_STATE_UPDATE_DELAY_MS = 500L
    }

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            updateStepUI()
        } else {
            showError(R.string.error_permission_denied)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Result not critical */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupWizardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        updateStepUI()
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        // Update UI in case permissions or service state changed
        updateStepUI()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupClickListeners() {
        binding.btnAction.setOnClickListener { performStepAction() }
        binding.btnContinue.setOnClickListener { goToNextStep() }
        binding.btnBack.setOnClickListener { goToPreviousStep() }
    }

    private fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == 
            PackageManager.PERMISSION_GRANTED
    }

    private fun isServiceRunning(): Boolean {
        return WakeWordService.isRunning(this)
    }

    private fun updateStepUI() {
        // Update step indicator dots (only show 3 dots now)
        val dots = listOf(
            binding.step1Dot,
            binding.step2Dot,
            binding.step3Dot
        )
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index < currentStep) R.drawable.step_indicator_active
                else R.drawable.step_indicator_inactive
            )
        }
        
        // Hide unused dots
        binding.step4Dot.visibility = View.GONE
        binding.step5Dot.visibility = View.GONE

        // Update back button visibility
        binding.btnBack.visibility = if (currentStep > 1) View.VISIBLE else View.INVISIBLE

        // Update content based on current step
        when (currentStep) {
            1 -> setupStep1()
            2 -> setupStep2()
            3 -> setupStep3()
            4 -> setupComplete()
        }
    }

    private fun setupStep1() {
        binding.tvStepTitle.text = getString(R.string.step1_title)
        binding.btnContinue.text = getString(R.string.button_continue)
        binding.ivIcon.setImageResource(R.drawable.ic_step_default)
        binding.stepIndicator.visibility = View.VISIBLE
        
        if (hasMicrophonePermission()) {
            binding.tvStepDescription.text = getString(R.string.step1_permission_granted)
            binding.btnAction.visibility = View.GONE
        } else {
            binding.tvStepDescription.text = getString(R.string.step1_description)
            binding.btnAction.text = getString(R.string.step1_button_grant)
            binding.btnAction.visibility = View.VISIBLE
        }
    }

    private fun setupStep2() {
        binding.tvStepTitle.text = getString(R.string.step2_title)
        binding.tvStepDescription.text = getString(R.string.step2_description)
        binding.btnAction.text = getString(R.string.step2_button_disable)
        binding.btnAction.visibility = View.VISIBLE
        binding.btnContinue.text = getString(R.string.button_continue)
        binding.ivIcon.setImageResource(R.drawable.ic_step_default)
    }

    private fun setupStep3() {
        binding.tvStepTitle.text = getString(R.string.step3_title)
        binding.ivIcon.setImageResource(R.drawable.ic_step_default)
        binding.btnContinue.text = getString(R.string.button_done)
        
        if (isServiceRunning()) {
            binding.tvStepDescription.text = getString(R.string.step3_status_running)
            binding.btnAction.text = getString(R.string.step3_button_stop)
        } else {
            binding.tvStepDescription.text = getString(R.string.step3_description)
            binding.btnAction.text = getString(R.string.step3_button_start)
        }
        binding.btnAction.visibility = View.VISIBLE
    }

    private fun setupComplete() {
        binding.tvStepTitle.text = getString(R.string.setup_complete_title)
        binding.tvStepDescription.text = getString(R.string.setup_complete_description)
        binding.btnAction.text = getString(R.string.button_battery_settings)
        binding.btnAction.visibility = View.VISIBLE
        binding.btnContinue.text = getString(R.string.button_done)
        binding.btnBack.visibility = View.INVISIBLE
        binding.ivIcon.setImageResource(R.drawable.ic_check)
        
        // Hide step indicators for completion screen
        binding.stepIndicator.visibility = View.GONE
    }

    private fun performStepAction() {
        when (currentStep) {
            1 -> requestMicrophonePermission()
            2 -> openBatteryOptimizationSettings()
            3 -> toggleWakeWordService()
            4 -> openBatteryOptimizationSettings()
        }
    }

    private fun goToNextStep() {
        if (currentStep <= totalSteps) {
            // Validate current step before proceeding
            when (currentStep) {
                1 -> {
                    if (!hasMicrophonePermission()) {
                        showError(R.string.error_permission_denied)
                        return
                    }
                }
            }
            currentStep++
            updateStepUI()
        } else {
            // Done - finish the activity
            finish()
        }
    }

    private fun goToPreviousStep() {
        if (currentStep > 1) {
            currentStep--
            updateStepUI()
        }
    }

    private fun requestMicrophonePermission() {
        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun toggleWakeWordService() {
        if (isServiceRunning()) {
            WakeWordService.stop(this)
        } else {
            if (!hasMicrophonePermission()) {
                showError(R.string.error_permission_denied)
                return
            }
            WakeWordService.start(this)
        }
        // Small delay to let service state update
        binding.btnAction.postDelayed({
            updateStepUI()
        }, SERVICE_STATE_UPDATE_DELAY_MS)
    }

    private fun openBatteryOptimizationSettings() {
        try {
            // Try to request exemption for this app specifically
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = getSystemService(POWER_SERVICE) as PowerManager
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    return
                }
            }
            
            // Fallback to general battery optimization settings
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general battery settings
            try {
                val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    getString(R.string.battery_optimization_manual),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showError(messageResId: Int) {
        Toast.makeText(this, getString(messageResId), Toast.LENGTH_LONG).show()
    }
}

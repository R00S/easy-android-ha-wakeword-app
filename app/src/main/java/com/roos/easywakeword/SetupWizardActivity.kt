package com.roos.easywakeword

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.roos.easywakeword.databinding.ActivitySetupWizardBinding
import java.io.File
import java.io.FileOutputStream

class SetupWizardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupWizardBinding
    private var currentStep = 1
    private val totalSteps = 5

    // Package names
    companion object {
        const val HOTWORD_PLUGIN_PACKAGE = "com.pocketsphinx.hotword"
        const val AUTOMATE_PACKAGE = "com.llamalab.automate"
        const val HOME_ASSISTANT_PACKAGE = "io.homeassistant.companion.android"
        
        // Asset file names
        const val AUTOMATE_FLOW_FILE = "ha_wakeword.flo"
        const val WAKEWORD_MODEL_FILE = "hey_mycroft.tflite"
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupClickListeners() {
        binding.btnAction.setOnClickListener { performStepAction() }
        binding.btnContinue.setOnClickListener { goToNextStep() }
        binding.btnBack.setOnClickListener { goToPreviousStep() }
    }

    private fun updateStepUI() {
        // Update step indicator dots
        val dots = listOf(
            binding.step1Dot,
            binding.step2Dot,
            binding.step3Dot,
            binding.step4Dot,
            binding.step5Dot
        )
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index < currentStep) R.drawable.step_indicator_active
                else R.drawable.step_indicator_inactive
            )
        }

        // Update back button visibility
        binding.btnBack.visibility = if (currentStep > 1) View.VISIBLE else View.INVISIBLE

        // Update content based on current step
        when (currentStep) {
            1 -> setupStep1()
            2 -> setupStep2()
            3 -> setupStep3()
            4 -> setupStep4()
            5 -> setupStep5()
            6 -> setupComplete()
        }
    }

    private fun setupStep1() {
        binding.tvStepTitle.text = getString(R.string.step1_title)
        binding.tvStepDescription.text = getString(R.string.step1_description)
        binding.btnAction.text = getString(R.string.step1_button_install)
        binding.btnAction.visibility = View.VISIBLE
        binding.btnContinue.text = getString(R.string.button_continue)
        binding.ivIcon.setImageResource(R.drawable.ic_download)
    }

    private fun setupStep2() {
        binding.tvStepTitle.text = getString(R.string.step2_title)
        binding.tvStepDescription.text = getString(R.string.step2_description)
        binding.btnAction.text = getString(R.string.step2_button_install)
        binding.btnAction.visibility = View.VISIBLE
        binding.btnContinue.text = getString(R.string.button_continue)
        binding.ivIcon.setImageResource(R.drawable.ic_download)
    }

    private fun setupStep3() {
        binding.tvStepTitle.text = getString(R.string.step3_title)
        binding.tvStepDescription.text = getString(R.string.step3_description)
        binding.btnAction.text = getString(R.string.step3_button_import)
        binding.btnAction.visibility = View.VISIBLE
        binding.btnContinue.text = getString(R.string.button_continue)
        binding.ivIcon.setImageResource(R.drawable.ic_step_default)
    }

    private fun setupStep4() {
        binding.tvStepTitle.text = getString(R.string.step4_title)
        binding.tvStepDescription.text = getString(R.string.step4_description)
        binding.btnAction.text = getString(R.string.step4_button_open)
        binding.btnAction.visibility = View.VISIBLE
        binding.btnContinue.text = getString(R.string.button_continue)
        binding.ivIcon.setImageResource(R.drawable.ic_step_default)
    }

    private fun setupStep5() {
        binding.tvStepTitle.text = getString(R.string.step5_title)
        binding.tvStepDescription.text = getString(R.string.step5_description)
        binding.btnAction.text = getString(R.string.step5_button_import)
        binding.btnAction.visibility = View.VISIBLE
        binding.btnContinue.text = getString(R.string.button_done)
        binding.ivIcon.setImageResource(R.drawable.ic_step_default)
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
            1 -> openPlayStore(HOTWORD_PLUGIN_PACKAGE)
            2 -> openPlayStore(AUTOMATE_PACKAGE)
            3 -> importAutomateFlow()
            4 -> openAutomate()
            5 -> importWakeWordModel()
            6 -> openBatteryOptimizationSettings()
        }
    }

    private fun goToNextStep() {
        if (currentStep <= totalSteps) {
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

    private fun openPlayStore(packageName: String) {
        try {
            // Try Play Store app first
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback to web browser
            try {
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                }
                startActivity(webIntent)
            } catch (e: Exception) {
                showError(R.string.error_install_failed)
            }
        }
    }

    private fun importAutomateFlow() {
        try {
            val file = copyAssetToCache(AUTOMATE_FLOW_FILE)
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/octet-stream")
                setPackage(AUTOMATE_PACKAGE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Try alternative import method
            try {
                val file = copyAssetToCache(AUTOMATE_FLOW_FILE)
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    setPackage(AUTOMATE_PACKAGE)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            } catch (e: Exception) {
                showError(R.string.error_import_failed)
            }
        }
    }

    private fun openAutomate() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(AUTOMATE_PACKAGE)
            if (intent != null) {
                startActivity(intent)
            } else {
                openPlayStore(AUTOMATE_PACKAGE)
            }
        } catch (e: Exception) {
            showError(R.string.error_import_failed)
        }
    }

    private fun importWakeWordModel() {
        try {
            val file = copyAssetToCache(WAKEWORD_MODEL_FILE)
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            // Try Hotword Plugin's import intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/octet-stream")
                setPackage(HOTWORD_PLUGIN_PACKAGE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback: try sharing the model file
            try {
                val file = copyAssetToCache(WAKEWORD_MODEL_FILE)
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    setPackage(HOTWORD_PLUGIN_PACKAGE)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            } catch (e: Exception) {
                showError(R.string.error_import_failed)
            }
        }
    }

    private fun copyAssetToCache(assetName: String): File {
        val file = File(cacheDir, assetName)
        if (!file.exists()) {
            assets.open(assetName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file
    }

    private fun openBatteryOptimizationSettings() {
        try {
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
                    "Please disable battery optimization manually in Settings",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showError(messageResId: Int) {
        Toast.makeText(this, getString(messageResId), Toast.LENGTH_LONG).show()
    }
}

package com.micoverride

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.micoverride.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var audioManager: AudioManager
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var isServiceActive = false

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_HEADSET_PLUG) {
                val state = intent.getIntExtra("state", -1)
                when (state) {
                    1 -> { if (isServiceActive) forceBuiltinMicSystemWide() }
                    0 -> updateStatus("🔌 Kit débranché\n✅ Micro intégré par défaut")
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.RECORD_AUDIO] == true) initApp()
            else Toast.makeText(this, "Permission micro requise", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        checkPermissions()
        setupUI()
    }

    private fun setupUI() {
        binding.btnToggleService.setOnClickListener {
            if (!isServiceActive) startMicService() else stopMicService()
        }
        binding.btnTestMic.setOnClickListener {
            if (!isRecording) startTestRecording() else stopTestRecording()
        }
        binding.btnRefreshDevices.setOnClickListener { listAudioDevices() }

        binding.btnOpenGoogleTranslate.setOnClickListener {
            forceBuiltinMicSystemWide()
            updateStatus("✅ Override activé\n🚀 Ouverture de Google Translate...")
            launchApp("com.google.android.apps.translate")
        }
        binding.btnOpenMsTranslator.setOnClickListener {
            forceBuiltinMicSystemWide()
            updateStatus("✅ Override activé\n🚀 Ouverture de Microsoft Translator...")
            launchApp("com.microsoft.translator")
        }
        binding.btnAddTile.setOnClickListener { showTileInstructions() }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED)
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        else initApp()
    }

    private fun initApp() {
        listAudioDevices()
        registerReceiver(headsetReceiver, IntentFilter(AudioManager.ACTION_HEADSET_PLUG))
        updateStatus("⏸️ Service inactif\n\n💡 Conseil :\nActivez l'override AVANT d'utiliser\nGoogle Translate ou MS Translator\n\nOu utilisez les boutons de lancement\ndirects ci-dessous ↓")
    }

    private fun startMicService() {
        isServiceActive = true
        binding.btnToggleService.text = "🔴 DÉSACTIVER"
        binding.btnToggleService.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
        ContextCompat.startForegroundService(this, Intent(this, MicOverrideService::class.java))
        forceBuiltinMicSystemWide()
        updateStatus("✅ Override ACTIF\n\nOuvrez maintenant :\n• Google Translate 🌐\n• Microsoft Translator 🔵\n\nLe micro intégré sera utilisé.")
    }

    private fun stopMicService() {
        isServiceActive = false
        binding.btnToggleService.text = "🟢 ACTIVER"
        binding.btnToggleService.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
        stopService(Intent(this, MicOverrideService::class.java))
        restoreDefaultRouting()
        updateStatus("⏸️ Override désactivé\nRoutage audio restauré")
    }

    /**
     * System-wide override — affects Google Translate, MS Translator, and all apps.
     *
     * 1. MODE_IN_COMMUNICATION  = tells Android to prefer built-in bottom mic
     * 2. setCommunicationDevice = Android 12+ explicit selection
     * 3. setParameters          = HAL hint for Qualcomm/MediaTek chips
     */
    fun forceBuiltinMicSystemWide() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val builtinMic = audioManager
                .getDevices(AudioManager.GET_DEVICES_INPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
            builtinMic?.let { audioManager.setCommunicationDevice(it) }
        }

        try { audioManager.setParameters("input_source=BUILTIN_MIC") } catch (_: Exception) {}
        listAudioDevices()
    }

    private fun restoreDefaultRouting() {
        audioManager.mode = AudioManager.MODE_NORMAL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) audioManager.clearCommunicationDevice()
        try { audioManager.setParameters("input_source=DEFAULT") } catch (_: Exception) {}
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "App non installée : $packageName", Toast.LENGTH_LONG).show()
            try {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("market://details?id=$packageName")
                })
            } catch (_: Exception) {}
        }
    }

    private fun showTileInstructions() {
        updateStatus(
            "📋 Ajouter la tuile Réglages rapides :\n\n" +
            "1. Faites glisser 2× depuis le haut\n" +
            "2. Appuyez sur ✏️ (modifier les tuiles)\n" +
            "3. Cherchez « Micro intégré »\n" +
            "4. Glissez-la vers vos tuiles actives\n\n" +
            "✅ Ensuite : 1 tap suffit avant d'ouvrir\n" +
            "Google Translate ou MS Translator !"
        )
    }

    private fun startTestRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return
        val bufferSize = AudioRecord.getMinBufferSize(44100,
            android.media.AudioFormat.CHANNEL_IN_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, 44100,
            android.media.AudioFormat.CHANNEL_IN_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                ?.let { audioRecord?.setPreferredDevice(it) }
        }
        audioRecord?.startRecording()
        isRecording = true
        binding.btnTestMic.text = "⏹️ Arrêter"
        updateStatus("🎙️ Test en cours...\nParlez dans le bas du téléphone\n(micro intégré)")
    }

    private fun stopTestRecording() {
        audioRecord?.stop(); audioRecord?.release(); audioRecord = null
        isRecording = false
        binding.btnTestMic.text = "🎙️ Tester le micro"
        updateStatus("✅ Test terminé")
    }

    private fun listAudioDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val sb = StringBuilder("📥 Entrées détectées :\n")
            audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).forEach { d ->
                val active = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    audioManager.communicationDevice?.id == d.id) " ← ACTIF" else ""
                sb.append("  • ${getDeviceTypeName(d.type)}$active\n")
            }
            binding.tvDeviceList.text = sb.toString()
        }
    }

    private fun getDeviceTypeName(type: Int): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC      -> "🎙️ Micro intégré"
            AudioDeviceInfo.TYPE_WIRED_HEADSET    -> "🎧 Kit filaire"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "🎧 Casque filaire"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO    -> "🔵 Bluetooth appels"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP   -> "🔵 Bluetooth audio"
            AudioDeviceInfo.TYPE_USB_DEVICE       -> "🔌 USB Audio"
            else -> "❓ type=$type"
        } else "Inconnu"

    private fun updateStatus(msg: String) = runOnUiThread { binding.tvStatus.text = msg }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(headsetReceiver) } catch (_: Exception) {}
        stopTestRecording()
    }
}

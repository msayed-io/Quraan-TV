package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.screens.QuranTvMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.QuranBgBlack
import com.example.viewmodel.QuranPlayerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: QuranPlayerViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep TV screen awake during audio playback & Quran display
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Android 9 (API 28) & forward Runtime Permission Check
        checkAndRequestStoragePermission()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = QuranBgBlack
                ) {
                    QuranTvMainScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun checkAndRequestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(isGranted)

        if (!isGranted) {
            requestPermissionLauncher.launch(permission)
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-verify permission state if user altered settings in background
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (isGranted && !viewModel.uiState.value.hasStoragePermission) {
            viewModel.onPermissionResult(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release player only if not actively playing in background service
        if (!viewModel.uiState.value.isPlaying) {
            viewModel.releasePlayer()
        }
    }
}



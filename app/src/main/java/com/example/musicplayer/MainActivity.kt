@file:OptIn(UnstableApi::class)

package com.example.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.media.audiofx.Equalizer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

class MainActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var equalizer: Equalizer? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) setupPlayerAndPlay()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context) else darkColorScheme()

            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Box(modifier = Modifier.fillMaxSize().blur(16.dp).background(Color.White.copy(alpha = 0.05f)))
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                        Text("Auto-Play Music", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Mencari dan memutar file audio secara otomatis...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            setupPlayerAndPlay()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun setupPlayerAndPlay() {
        if (player == null) {
            player = ExoPlayer.Builder(this).build().apply {
                experimentalSetOffloadSchedulingEnabled(true)
            }
            equalizer = Equalizer(0, player!!.audioSessionId).apply { enabled = true }
        }

        val audioList = mutableListOf<MediaItem>()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA)
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val pathCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (it.moveToNext()) {
                audioList.add(MediaItem.fromUri(it.getString(pathCol)))
            }
        }

        player?.apply {
            setMediaItems(audioList)
            prepare()
            play()
        }
    }

    override fun onDestroy() {
        equalizer?.release()
        player?.release()
        super.onDestroy()
    }
}

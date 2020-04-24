package com.masterconcept.audio_recorder_mc_example

import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity

class MainActivity: FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        Log.i("Main", "Entrou main")

        if (!checkPermissions()) {
            requestPermissions()
        }
    }
    private fun checkPermissions(): Boolean {
        val result1 = ContextCompat.checkSelfPermission(applicationContext, RECORD_AUDIO)
        return  result1 == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(RECORD_AUDIO, WRITE_EXTERNAL_STORAGE), 1)
    }
}

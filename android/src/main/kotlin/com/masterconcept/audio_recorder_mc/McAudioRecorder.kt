package com.masterconcept.audio_recorder_mc

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import io.flutter.Log
import io.flutter.plugin.common.EventChannel
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean


class McAudioRecorder : Runnable {
    private var LOG_TAG = "Microphone"
    var mainHandler: Handler = Handler(Looper.getMainLooper())

    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
    private val BUFFER_SIZE_FACTOR = 1

    private var SAMPLING_RATE_IN_HZ = 44100
    private var BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR

    private val recordingInProgress: AtomicBoolean = AtomicBoolean(false)

    private var recorder: AudioRecord? = null

    private var recordingThread: Thread? = null

    var mEventSink: EventChannel.EventSink? = null

    fun setRate(sampleRate: Int) {
        SAMPLING_RATE_IN_HZ = sampleRate
        BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR
    }

    fun startRecording() {
        recorder = AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE)
        recorder!!.startRecording()
        recordingInProgress.set(true)
        recordingThread = Thread(this, "Recording Thread")
        recordingThread!!.start()
    }

    fun stopRecording() {
        if (null == recorder) {
            return
        }
        recordingInProgress.set(false)
        recorder!!.stop()
        recorder!!.release()
        recorder = null
        recordingThread = null
    }

    private fun getBufferReadFailureReason(errorCode: Int): String {
        return when (errorCode) {
            AudioRecord.ERROR_INVALID_OPERATION -> "ERROR_INVALID_OPERATION"
            AudioRecord.ERROR_BAD_VALUE -> "ERROR_BAD_VALUE"
            AudioRecord.ERROR_DEAD_OBJECT -> "ERROR_DEAD_OBJECT"
            AudioRecord.ERROR -> "ERROR"
            else -> "Unknown ($errorCode)"
        }
    }

    @UiThread
    fun addSample(samples: FloatArray, index: Int) {
//        Log.i("teste", samples.size.toString())
//        Log.i("teste", samples.toString())
//        if (index == 0) {
//            Log.i("teste", "Entrou")
//            var str = ""

//            Log.i("teste", str)
//            samples.forEach { Log.i("teste", it.toString()); }
//        }


        if (this.mEventSink != null) {
            this.mEventSink!!.success(samples.toList())
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun run() {
        val buffer: FloatArray = FloatArray(BUFFER_SIZE)
        var i = 0
        try {

            while (recordingInProgress.get()) {
                val result: Int = recorder!!.read(buffer, 0, BUFFER_SIZE, AudioRecord.READ_BLOCKING)
                if (result < 0) {
                    throw RuntimeException("Reading of audio buffer failed: " +
                            getBufferReadFailureReason(result))
                }

                mainHandler.post { addSample(buffer, i) }

                i += 1
            }
        } catch (e: IOException) {
            throw RuntimeException("Writing of recorded audio failed", e)
        }
    }
}
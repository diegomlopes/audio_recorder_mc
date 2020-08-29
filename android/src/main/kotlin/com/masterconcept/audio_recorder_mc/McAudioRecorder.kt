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

    private var BUFFER_SIZE_FACTOR = 1

    private var CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private var AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
    private var SAMPLING_RATE_IN_HZ = 44100
    private var BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR

    private val recordingInProgress: AtomicBoolean = AtomicBoolean(false)

    private var recorder: AudioRecord? = null

    private var recordingThread: Thread? = null

    var mEventSink: EventChannel.EventSink? = null

    fun getBufferSize(): Int {
        return BUFFER_SIZE
    }

    fun setRate(sampleRate: Int) {
        SAMPLING_RATE_IN_HZ = sampleRate
        BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR
    }

    fun setBits(bits: Int) {
        when (bits) {
            8 -> AUDIO_FORMAT = AudioFormat.ENCODING_PCM_8BIT
            16 -> AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
            else -> AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
        }
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
    fun addFloatSample(samples: FloatArray, index: Int) {
        if (this.mEventSink != null) {
            this.mEventSink!!.success(samples.toList())
        }
    }

    @UiThread
    fun addShortSample(samples: ShortArray, index: Int) {
        if (this.mEventSink != null) {
            this.mEventSink!!.success(samples.toList())
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun run() {
        try {
        if (AUDIO_FORMAT == AudioFormat.ENCODING_PCM_FLOAT) {
            val buffer: FloatArray = FloatArray(BUFFER_SIZE)
            var i = 0
            while (recordingInProgress.get()) {
                val result: Int = recorder!!.read(buffer, 0, BUFFER_SIZE, AudioRecord.READ_BLOCKING)
                if (result < 0) {
                    throw RuntimeException("Reading of audio buffer failed: " +
                            getBufferReadFailureReason(result))
                }
                mainHandler.post { addFloatSample(buffer, i) }
                i += 1
            }
        } else {
            val buffer: ShortArray = ShortArray(BUFFER_SIZE)
            var i = 0
            while (recordingInProgress.get()) {
                val result: Int = recorder!!.read(buffer, 0, BUFFER_SIZE, AudioRecord.READ_BLOCKING)
                if (result < 0) {
                    throw RuntimeException("Reading of audio buffer failed: " +
                            getBufferReadFailureReason(result))
                }
                mainHandler.post { addShortSample(buffer, i) }
                i += 1
            }
        }

        } catch (e: IOException) {
            throw RuntimeException("Writing of recorded audio failed", e)
        }
    }
}
package com.masterconcept.audio_recorder_mc

import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar


/** AudioRecorderMcPlugin */
public class AudioRecorderMcPlugin: FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var setupChannel : MethodChannel
  private lateinit var startRecordChannel : MethodChannel
  private lateinit var stopRecordChannel : MethodChannel
  private lateinit var samplesRecordChannel : EventChannel
  private var mEventSink: EventChannel.EventSink? = null
  private var recorder = McAudioRecorder()

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    setupChannel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "com.masterconcept.audiorecorder/setup")
    startRecordChannel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "com.masterconcept.audiorecorder/start")
    stopRecordChannel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "com.masterconcept.audiorecorder/stop")
    samplesRecordChannel = EventChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "com.masterconcept.audiorecorder/samples")

    setupChannel.setMethodCallHandler(this);
    startRecordChannel.setMethodCallHandler(this);
    stopRecordChannel.setMethodCallHandler(this);
    samplesRecordChannel.setStreamHandler(this);
  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      Log.i("audio_recorder_mc_debug", "begin method registration")
      val setupChannel = MethodChannel(registrar.messenger(), "com.masterconcept.audiorecorder/setup")
      val setFormatChannel = MethodChannel(registrar.messenger(), "com.masterconcept.audiorecorder/setFormat")
      val startRecordChannel = MethodChannel(registrar.messenger(), "com.masterconcept.audiorecorder/start")
      val stopRecordChannel = MethodChannel(registrar.messenger(), "com.masterconcept.audiorecorder/stop")
      var samplesRecordChannel = EventChannel(registrar.messenger(), "com.masterconcept.audiorecorder/samples")

      var instance = AudioRecorderMcPlugin()

      setupChannel.setMethodCallHandler(instance)
      startRecordChannel.setMethodCallHandler(instance)
      stopRecordChannel.setMethodCallHandler(instance)
      samplesRecordChannel.setStreamHandler(instance)
    }

  }

  @RequiresApi(Build.VERSION_CODES.M)
  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "com.masterconcept.audiorecorder/start") {
      Log.i("audio_recorder_mc_debug", "begin recording start")
      recorder.startRecording()
      result.success("Recording")
      Log.i("audio_recorder_mc_debug", "recording started")
    }
    else if (call.method == "com.masterconcept.audiorecorder/stop") {
      Log.i("audio_recorder_mc_debug", "begin recording stop")
      recorder.stopRecording()
      result.success("Stopped")
      Log.i("audio_recorder_mc_debug", "recording stopped")
    }
    else if (call.method == "com.masterconcept.audiorecorder/setup") {
      val rate = call.argument<Int>("sampleRate") // actually returns an Int?
      if (rate is Int) {
        recorder.setRate(rate)
        Log.i("audio_recorder_mc_debug", "sample rate set to $rate")
      }

      val format = call.argument<Int>("sampleFormat") // actually returns an Int?
      if (format is Int) {
        var formatString :String
        if (format == 0) {
          formatString = "pcm_8bit"
          recorder.setBits(8)
        } else if (format == 1) {
          formatString = "pcm_16bit"
          recorder.setBits(16)
        } else {
          formatString = "pcm_32float"
          recorder.setBits(32)
        }
        Log.i("audio_recorder_mc_debug", "sample format set to $formatString")

      }
      val bufSize = recorder.getBufferSize()
      Log.i("audio_recorder_mc_debug", "buffer is $bufSize bytes")
      result.success(bufSize)
    }
    else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    startRecordChannel.setMethodCallHandler(null)
    stopRecordChannel.setMethodCallHandler(null)
    samplesRecordChannel.setStreamHandler(null)
  }

  override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
    if (events != null) {
      recorder.mEventSink = events
      Log.i("audio_recorder_mc_debug", "beginning event stream")
      mEventSink = events
    }
  }

  override fun onCancel(arguments: Any?) {
    mEventSink = null
  }
}

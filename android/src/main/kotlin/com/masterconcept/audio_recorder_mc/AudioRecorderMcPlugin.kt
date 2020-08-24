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
  private lateinit var startRecordChannel : MethodChannel
  private lateinit var stopRecordChannel : MethodChannel
  private lateinit var samplesRecordChannel : EventChannel
  private var mEventSink: EventChannel.EventSink? = null
  private var recorder = McAudioRecorder()

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    startRecordChannel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "com.masterconcept.audiorecorder/start")
    stopRecordChannel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "com.masterconcept.audiorecorder/stop")
    samplesRecordChannel = EventChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "com.masterconcept.audiorecorder/samples")

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
      Log.i("test", "begin method registration")
      val initChannel = MethodChannel(registrar.messenger(), "com.masterconcept.audiorecorder/init")
      val startRecordChannel = MethodChannel(registrar.messenger(), "com.masterconcept.audiorecorder/start")
      val stopRecordChannel = MethodChannel(registrar.messenger(), "com.masterconcept.audiorecorder/stop")
      var samplesRecordChannel = EventChannel(registrar.messenger(), "com.masterconcept.audiorecorder/samples")

      var instance = AudioRecorderMcPlugin()

      initChannel.setMethodCallHandler(instance)
      startRecordChannel.setMethodCallHandler(instance)
      stopRecordChannel.setMethodCallHandler(instance)
      samplesRecordChannel.setStreamHandler(instance)
    }

  }

  @RequiresApi(Build.VERSION_CODES.M)
  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "com.masterconcept.audiorecorder/start") {
      Log.i("test", "begin recording start")
      recorder.startRecording()
      result.success("Recording")
      Log.i("test", "recording started")
    }
    else if (call.method == "com.masterconcept.audiorecorder/stop") {
      Log.i("test", "begin recording stop")
      recorder.stopRecording()
      result.success("Stopped")
      Log.i("test", "recording stopped")
    }
    else if (call.method == "com.masterconcept.audiorecorder/init") {
      val rate = call.arguments['sampleRate']
      Log.i("test", "initializing with sample rate of $rate")
      recorder.setRate(rate)
      result.success("Success")
      Log.i("test", "initialized with sample rate")
    }
    else {
      result.notImplemented()
    }

//    if (mEventSink != null) {
//    mEventSink.success(sample)
//    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    startRecordChannel.setMethodCallHandler(null)
    stopRecordChannel.setMethodCallHandler(null)
    samplesRecordChannel.setStreamHandler(null)
  }

  override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
    if (events != null) {
      recorder.mEventSink = events
      Log.i("teste", "beginning event stream")
      mEventSink = events
    }
  }

  override fun onCancel(arguments: Any?) {
    mEventSink = null
  }
}

import Flutter
import UIKit

public class SwiftAudioRecorderMcPlugin: NSObject, FlutterPlugin {
  var recorder = MCAudioRecorder()
      var eventSink: FlutterEventSink?

      public static func register(with registrar: FlutterPluginRegistrar) {
          let startRecordChannel = FlutterMethodChannel(name: "com.masterconcept.audiorecorder/start", binaryMessenger: registrar.messenger())
          let stopRecordChannel = FlutterMethodChannel(name: "com.masterconcept.audiorecorder/stop", binaryMessenger: registrar.messenger())
          let samplesRecordChannel = FlutterEventChannel(name: "com.masterconcept.audiorecorder/samples", binaryMessenger: registrar.messenger())
          
          let instance = SwiftAudioRecorderMcPlugin()
          
          registrar.addMethodCallDelegate(instance, channel: startRecordChannel)
          registrar.addMethodCallDelegate(instance, channel: stopRecordChannel)
          samplesRecordChannel.setStreamHandler(instance)
      }
      
      public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
          if call.method == "com.masterconcept.audiorecorder/start" {
              recorder = MCAudioRecorder()
              recorder.delegate = self
              recorder.startRecording()
              result("iOS - MC Audio Record Started")
          }
          if call.method == "com.masterconcept.audiorecorder/stop" {
              recorder.delegate = nil
              recorder.stopRecording()
              result("iOS - MC Audio Record Stopped")
          }
      }
      
  }

  extension SwiftAudioRecorderMcPlugin: MCAudioRecorderDelegate {
      func buffer(_ samples: [Float]) {
          if eventSink != nil {
              eventSink!(samples)
          }
      }
  }

  extension SwiftAudioRecorderMcPlugin: FlutterStreamHandler {
      
      public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
          eventSink = events
          return nil
      }
      
      public func onCancel(withArguments arguments: Any?) -> FlutterError? {
          eventSink = nil
          return nil
      }
  }


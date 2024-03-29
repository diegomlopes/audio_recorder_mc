import 'dart:async';
import 'dart:io';
import 'dart:math';

import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

enum AudioRecorderFormat { pcm8, pcm16, pcmFloat }

/// default sampleRate is 44100
/// Records using single channel, pcm float
class AudioRecorderMc {
  Future<bool> ready;

  int _sampleRate = 44100;
  int get sampleRate => _sampleRate;

  int _sampleBytes = 4;
  int get sampleBytes => _sampleBytes;

  int _bufSizeInBytes;
  int get bufSizeInBytes => _bufSizeInBytes;
  int get bufSizeInSamples => _bufSizeInBytes ~/ sampleBytes;
  double get bufSizeInMillis => 1000 * bufSizeInSamples / _sampleRate;

  AudioRecorderMc({
    int sampleRate = 44100,
    AudioRecorderFormat sampleFormat = AudioRecorderFormat.pcmFloat,
  }) {
    // this will populate the buffer size variable
    if (Platform.isAndroid)
      ready = setup(sampleRate: sampleRate, sampleFormat: sampleFormat);
  }

  static const MethodChannel _setupChannel =
      const MethodChannel('com.masterconcept.audiorecorder/setup');
  static const MethodChannel _startRecordChannel =
      const MethodChannel('com.masterconcept.audiorecorder/start');
  static const MethodChannel _stopRecordChannel =
      const MethodChannel('com.masterconcept.audiorecorder/stop');

  static const EventChannel _eventChannel =
      const EventChannel('com.masterconcept.audiorecorder/samples');

  Future<bool> setup({
    int sampleRate = 44100,
    AudioRecorderFormat sampleFormat = AudioRecorderFormat.pcmFloat,
  }) async {
    _sampleRate = sampleRate;
    _sampleBytes = 8 * pow(2, sampleFormat.index);
    _bufSizeInBytes = await _setupChannel
        .invokeMethod('com.masterconcept.audiorecorder/setup', {
      'sampleRate': sampleRate,
      'sampleFormat': sampleFormat.index,
    });

    PermissionsService().requestMicrophonePermission(onPermissionDenied: () {
      print('Permission has been denied');
    });

    return true;
  }

  Future<Stream<dynamic>> get startRecord async {
    PermissionsService().requestMicrophonePermission(onPermissionDenied: () {
      print('Permission has been denied');
    });

    await _startRecordChannel
        .invokeMethod('com.masterconcept.audiorecorder/start');

    return _eventChannel.receiveBroadcastStream();
  }

  Future<String> get stopRecord async {
    final String log = await _stopRecordChannel
        .invokeMethod('com.masterconcept.audiorecorder/stop');
    return log;
  }
}

class PermissionsService {
  /// Requests the users permission to read their microphone.
  Future<bool> requestMicrophonePermission(
      {Function onPermissionDenied}) async {
    if (await Permission.microphone.request().isGranted) {
      return true;
    }

    // You can request multiple permissions at once.
    Map<Permission, PermissionStatus> statuses = await [
      Permission.microphone,
    ].request();

    return statuses[Permission.microphone].isGranted;
  }
}

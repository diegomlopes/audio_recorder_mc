import 'dart:async';

import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

/// default sampleRate is 44100
/// Records using single channel, pcm float
class AudioRecorderMc {
  AudioRecorderMc();

  static const MethodChannel _initChannel = const MethodChannel('com.masterconcept.audiorecorder/init');
  static const MethodChannel _startRecordChannel = const MethodChannel('com.masterconcept.audiorecorder/start');

  static const MethodChannel _stopRecordChannel = const MethodChannel('com.masterconcept.audiorecorder/stop');

  static const EventChannel _eventChannel = const EventChannel('com.masterconcept.audiorecorder/samples');

  /// sampleRate defaults to 44100
  Future setRate([int rate = 44100]) async {
    await _initChannel.invokeMethod('com.masterconcept.audiorecorder/init', {'sampleRate': rate});
  }

  Future<Stream<dynamic>> get startRecord async {
    PermissionsService().requestMicrophonePermission(onPermissionDenied: () {
      print('Permission has been denied');
    });

    await _startRecordChannel.invokeMethod('com.masterconcept.audiorecorder/start');

    return _eventChannel.receiveBroadcastStream();
  }

  Future<String> get stopRecord async {
    final String log = await _stopRecordChannel.invokeMethod('com.masterconcept.audiorecorder/stop');
    return log;
  }
}

class PermissionsService {
  final PermissionHandler _permissionHandler = PermissionHandler();

  Future<bool> _requestPermission(PermissionGroup permission) async {
    var result = await _permissionHandler.requestPermissions([permission]);
    if (result[permission] == PermissionStatus.granted) {
      return true;
    }
    return false;
  }

  /// Requests the users permission to read their contacts.
  Future<bool> requestMicrophonePermission({Function onPermissionDenied}) async {
    var granted = await _requestPermission(PermissionGroup.microphone);
    if (!granted) {
      onPermissionDenied();
    }
    return granted;
  }

  Future<bool> hasContactsPermission() async {
    return hasPermission(PermissionGroup.microphone);
  }

  Future<bool> hasPermission(PermissionGroup permission) async {
    var permissionStatus = await _permissionHandler.checkPermissionStatus(permission);
    return permissionStatus == PermissionStatus.granted;
  }
}

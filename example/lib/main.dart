import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:audio_recorder_mc/audio_recorder_mc.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool isRecording = false;

  @override
  void initState() {
    super.initState();
  }

  var recorder = AudioRecorderMc()..setup(sampleRate: 8820);
  Stream<double> samples;

  void startRecord() {
    recorder.startRecord.then((stream) {
      setState(() {
        isRecording = true;
      });

      stream.listen((samples) {
        print(samples);
      });
    });
  }

  void stopRecord() {
    recorder.stopRecord.then((value) {
      setState(() {
        isRecording = false;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: [
              RaisedButton(
                  onPressed: () {
                    this.isRecording ? stopRecord() : startRecord();
                  },
                  child: Text(this.isRecording ? 'Stop' : 'Start')),
            ],
          ),
        ),
      ),
    );
  }
}

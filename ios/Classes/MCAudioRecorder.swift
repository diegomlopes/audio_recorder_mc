//
//  McAudioRecorder.swift
//  mc_audio_recorder
//
//  Created by Diego Lopes on 23/04/20.
//

import Foundation
import AVFoundation
import AudioUnit

var gTmp0 = 0 //  temporary variable for debugger viewing

class MCAudioRecorder: NSObject {
    
    weak var delegate: MCAudioRecorderDelegate?
    var sinkOnChanged: FlutterEventSink?
    var listeningOnChanged = false
    
    private var audioUnit:   AudioUnit?
    private var micPermission   =  false
    private var sessionActive   =  false
    private var isRecording     =  false
    
    var sampleRate : Double = 44100.0    // default audio sample rate
    let circBuffSize = 32768        // lock-free circular fifo/buffer size
    var circBuffer   = [Float](repeating: 0, count: 32768)  // for incoming samples
    var circInIdx  : Int =  0
    var audioLevel : Float  = 0.0
    var bufferSize = 35280
    var indexProcess = 0
    private var hwSRate = 44100.0   // guess of device hardware sample rate
    private var micPermissionDispatchToken = 0
    private var interrupted = false     // for restart from audio interruption notification
    var numberOfChannels: Int       =  1
    
    private let outputBus: UInt32   =  0
    private let inputBus: UInt32    =  1
    
    private var temp: [Float] = []
    private var samples : [Float] = []
    
    var contAm = 0
    
    func startRecording() {
        indexProcess = 0
        if isRecording { return }
        
        startAudioSession()
//        if sessionActive {
            startAudioUnit()
//        }
    }
    
    func startAudioUnit() {
        var err: OSStatus = noErr
        
        temp.removeAll()
        samples.removeAll()
        
        if self.audioUnit == nil {
            setupAudioUnit()         // setup once
        }
        guard let au = self.audioUnit
            else { return }
        
        err = AudioUnitInitialize(au)
        gTmp0 = Int(err)
        if err != noErr { return }
        err = AudioOutputUnitStart(au)  // start
        
        gTmp0 = Int(err)
        if err == noErr {
            isRecording = true
        }
    }
    
    func startAudioSession() {
        if (sessionActive == false) {
            // set and activate Audio Session
            do {
                
                let audioSession = AVAudioSession.sharedInstance()
                
                if (micPermission == false) {
                    if (micPermissionDispatchToken == 0) {
                        micPermissionDispatchToken = 1
                        audioSession.requestRecordPermission({(granted: Bool)-> Void in
                            if granted {
                                self.micPermission = true
                                return
                                // check for this flag and call from UI loop if needed
                            } else {
                                gTmp0 += 1
                                // dispatch in main/UI thread an alert
                                //   informing that mic permission is not switched on
                            }
                        })
                    }
                }
                if micPermission == false { return }
                
                try audioSession.setMode(AVAudioSession.Mode.measurement)
                try audioSession.setCategory(AVAudioSession.Category.playAndRecord)
                // choose 44100 or 48000 based on hardware rate
                // sampleRate = 44100.0
                var preferredIOBufferDuration = 0.0058      // 5.8 milliseconds = 256 samples
                hwSRate = audioSession.sampleRate           // get native hardware rate
                if hwSRate == 44100.0 { sampleRate = 44100.0 }  // set session to hardware rate
                if hwSRate == 44100.0 { preferredIOBufferDuration = 0.0053 }
                //                let desiredSampleRate = sampleRate
                try audioSession.setPreferredSampleRate(44100.0)
                try audioSession.setPreferredIOBufferDuration(preferredIOBufferDuration)
                
                NotificationCenter.default.addObserver(
                    forName: AVAudioSession.interruptionNotification,
                    object: nil,
                    queue: nil,
                    using: myAudioSessionInterruptionHandler )
                
                try audioSession.setActive(true)
                sessionActive = true
            } catch /* let error as NSError */ {
                // handle error here
            }
        }
    }
    
    private func setupAudioUnit() {
        
        var componentDesc:  AudioComponentDescription
            = AudioComponentDescription(
                componentType:          OSType(kAudioUnitType_Output),
                componentSubType:       OSType(kAudioUnitSubType_RemoteIO),
                componentManufacturer:  OSType(kAudioUnitManufacturer_Apple),
                componentFlags:         UInt32(0),
                componentFlagsMask:     UInt32(0) )
        
        var osErr: OSStatus = noErr
        
        let component: AudioComponent! = AudioComponentFindNext(nil, &componentDesc)
        
        var tempAudioUnit: AudioUnit?
        osErr = AudioComponentInstanceNew(component, &tempAudioUnit)
        self.audioUnit = tempAudioUnit
        
        guard let au = self.audioUnit
            else { return }
        
        // Enable I/O for input.
        
        var one_ui32: UInt32 = 1
        
        osErr = AudioUnitSetProperty(au,
                                     kAudioOutputUnitProperty_EnableIO,
                                     kAudioUnitScope_Input,
                                     inputBus,
                                     &one_ui32,
                                     UInt32(MemoryLayout<UInt32>.size))
        
        // Set format to 32-bit Floats, linear PCM
        let nc = 1  // 2 channel stereo
        var streamFormatDesc:AudioStreamBasicDescription = AudioStreamBasicDescription(
            mSampleRate:        Double(sampleRate),
            mFormatID:          kAudioFormatLinearPCM,
            mFormatFlags:       ( kAudioFormatFlagsNativeFloatPacked ),
            mBytesPerPacket:    UInt32(nc * MemoryLayout<UInt32>.size),
            mFramesPerPacket:   1,
            mBytesPerFrame:     UInt32(nc * MemoryLayout<UInt32>.size),
            mChannelsPerFrame:  UInt32(nc),
            mBitsPerChannel:    UInt32(8 * (MemoryLayout<UInt32>.size)),
            mReserved:          UInt32(0)
        )
        
        osErr = AudioUnitSetProperty(au,
                                     kAudioUnitProperty_StreamFormat,
                                     kAudioUnitScope_Input, outputBus,
                                     &streamFormatDesc,
                                     UInt32(MemoryLayout<AudioStreamBasicDescription>.size))
        
        osErr = AudioUnitSetProperty(au,
                                     kAudioUnitProperty_StreamFormat,
                                     kAudioUnitScope_Output,
                                     inputBus,
                                     &streamFormatDesc,
                                     UInt32(MemoryLayout<AudioStreamBasicDescription>.size))
        
        var inputCallbackStruct
            = AURenderCallbackStruct(inputProc: recordingCallback,
                                     inputProcRefCon:
                UnsafeMutableRawPointer(Unmanaged.passUnretained(self).toOpaque()))
        
        osErr = AudioUnitSetProperty(au,
                                     AudioUnitPropertyID(kAudioOutputUnitProperty_SetInputCallback),
                                     AudioUnitScope(kAudioUnitScope_Global),
                                     inputBus,
                                     &inputCallbackStruct,
                                     UInt32(MemoryLayout<AURenderCallbackStruct>.size))
        
        // Ask CoreAudio to allocate buffers on render.
        osErr = AudioUnitSetProperty(au,
                                     AudioUnitPropertyID(kAudioUnitProperty_ShouldAllocateBuffer),
                                     AudioUnitScope(kAudioUnitScope_Output),
                                     inputBus,
                                     &one_ui32,
                                     UInt32(MemoryLayout<UInt32>.size))
        gTmp0 = Int(osErr)
    }
    
    let recordingCallback: AURenderCallback = { (
        inRefCon,
        ioActionFlags,
        inTimeStamp,
        inBusNumber,
        frameCount,
        ioData ) -> OSStatus in
        
        let audioObject = unsafeBitCast(inRefCon, to: MCAudioRecorder.self)
        var err: OSStatus = noErr
        
        // set mData to nil, AudioUnitRender() should be allocating buffers
        var bufferList = AudioBufferList(
            mNumberBuffers: 1,
            mBuffers: AudioBuffer(
                mNumberChannels: UInt32(2),
                mDataByteSize: 16,
                mData: nil))
        
        if let au = audioObject.audioUnit {
            err = AudioUnitRender(au,
                                  ioActionFlags,
                                  inTimeStamp,
                                  inBusNumber,
                                  frameCount,
                                  &bufferList)
        }
        
        audioObject.processMicrophoneBuffer( inputDataList: &bufferList,
                                             frameCount: UInt32(frameCount) )
        return 0
    }
    
    func processMicrophoneBuffer(inputDataList : UnsafeMutablePointer<AudioBufferList>, frameCount : UInt32) {
        let inputDataPtr = UnsafeMutableAudioBufferListPointer(inputDataList)
        let mBuffers : AudioBuffer = inputDataPtr[0]
        let count = Int(frameCount)
        
        let bufferPointer = UnsafeMutableRawPointer(mBuffers.mData)
        if let bptr = bufferPointer {
            let dataArray = bptr.assumingMemoryBound(to: Float.self)

            if delegate != nil {
                var floatArray = [Float]()
                
                for i in 0..<count {
                    floatArray.append(dataArray[i])
                }
                
                delegate?.buffer(floatArray)
            }
            // Mandar amostras aqui
//            for i in 0..<count {
//                contAm += 1
//                if contAm > 441*5 {
//                    temp.append(dataArray[i])
//                }
//            }
            
//            self.onChanged(data: bufferPointer.)
            
//            while temp.count > 0 {
//                samples.append(temp[0])
//                temp.remove(at: 0)
//                if samples.count == (bufferSize) {
//                    //Pegar as amostras aqui-------------------
//                    indexProcess += 1
//                    samples.removeAll()
//                }
//            }
        }
    }
    
    func stopRecording() {
        if (self.audioUnit != nil){
            AudioUnitUninitialize(self.audioUnit!)
        }
        isRecording = false
    }
    
    func myAudioSessionInterruptionHandler(notification: Notification) -> Void {
        let interuptionDict = notification.userInfo
        if let interuptionType = interuptionDict?[AVAudioSessionInterruptionTypeKey] {
            let interuptionVal = AVAudioSession.InterruptionType(
                rawValue: (interuptionType as AnyObject).uintValue )
            if (interuptionVal == AVAudioSession.InterruptionType.began) {
                if (isRecording) {
                    stopRecording()
                    isRecording = false
                    let audioSession = AVAudioSession.sharedInstance()
                    do {
                        try audioSession.setActive(false)
                        sessionActive = false
                    } catch {
                    }
                    interrupted = true
                }
            } else if (interuptionVal == AVAudioSession.InterruptionType.ended) {
                if (interrupted) {
                    // potentially restart here
                }
            }
        }
    }
}

extension MCAudioRecorder: FlutterStreamHandler {
    
    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        if let args = arguments as? Array<Any> {
            if args.count > 0 {
                if let eventName = args[0] as? String {
                    if eventName == "on_changed" {
                        self.sinkOnChanged  = events
                        listeningOnChanged = true
                    }
                }
            }
        }
        return nil
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        if let args = arguments as? Array<Any> {
            if args.count > 0 {
                if let eventName = args[0] as? String {
                    if eventName == "on_changed" {
                        listeningOnChanged = false
                    }
                }
            }
        }
        return nil
    }
    
    public func onChanged(data: Data) {
//        if listeningOnChanged {
//            if let sink = self.sinkOnChanged {
//                sink(data.)
//            }
//        }
    }
    
}

protocol MCAudioRecorderDelegate: class {
    func buffer(_ samples: [Float])
}

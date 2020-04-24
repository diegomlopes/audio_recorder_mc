#import "AudioRecorderMcPlugin.h"
#if __has_include(<audio_recorder_mc/audio_recorder_mc-Swift.h>)
#import <audio_recorder_mc/audio_recorder_mc-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "audio_recorder_mc-Swift.h"
#endif

@implementation AudioRecorderMcPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftAudioRecorderMcPlugin registerWithRegistrar:registrar];
}
@end

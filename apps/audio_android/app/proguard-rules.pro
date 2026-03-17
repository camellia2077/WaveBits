# JNI entrypoints are looked up by the generated Java method names.
-keep class com.bag.audioandroid.NativeBagBridge { *; }
# audio_io JNI also resolves bridge methods, metadata fields, enum accessors,
# and decoded result constructors by exact class/member names at runtime.
-keep class com.bag.audioandroid.NativeAudioIoBridge { *; }
-keep class com.bag.audioandroid.domain.GeneratedAudioMetadata { *; }
-keep class com.bag.audioandroid.domain.DecodedAudioData { *; }
-keep class com.bag.audioandroid.ui.model.TransportModeOption { *; }
-keep class com.bag.audioandroid.ui.model.FlashVoicingStyleOption { *; }

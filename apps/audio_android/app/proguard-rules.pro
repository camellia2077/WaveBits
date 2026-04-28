# JNI bridge entrypoints are looked up by the generated Java method names.
-keep class com.bag.audioandroid.NativeBagBridge { *; }
# audio_io JNI also resolves bridge methods, metadata fields, and constructors
# by exact class/member names at runtime.
-keep class com.bag.audioandroid.NativeAudioIoBridge { *; }

# Native metadata/dto bridges are also annotated with @Keep in source. Keep the
# rules here as a release-safety backstop for JNI descriptors and constructors.
-keep class com.bag.audioandroid.domain.GeneratedAudioMetadata { *; }
-keep class com.bag.audioandroid.domain.GeneratedAudioInputSourceKind { *; }
-keep class com.bag.audioandroid.domain.DecodedAudioData { *; }

# bag_api JNI builds payload/follow result objects with FindClass/NewObject,
# so these result models must keep their class names and constructor signatures.
-keep class com.bag.audioandroid.domain.EncodedAudioPayloadResult { *; }
-keep class com.bag.audioandroid.domain.DecodedAudioPayloadResult { *; }
-keep class com.bag.audioandroid.domain.DecodedPayloadViewData { *; }
-keep class com.bag.audioandroid.domain.PayloadFollowViewData { *; }
-keep class com.bag.audioandroid.domain.PayloadFollowByteTimelineEntry { *; }
-keep class com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry { *; }
-keep class com.bag.audioandroid.domain.TextFollowTimelineEntry { *; }
-keep class com.bag.audioandroid.domain.TextFollowRawSegmentViewData { *; }
-keep class com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData { *; }
-keep class com.bag.audioandroid.domain.TextFollowLyricLineTimelineEntry { *; }
-keep class com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData { *; }
-keep class com.bag.audioandroid.domain.TextFollowLineRawSegmentViewData { *; }

# Native metadata encoding/decoding reads enum accessors and names directly.
-keep class com.bag.audioandroid.ui.model.TransportModeOption { *; }
-keep class com.bag.audioandroid.ui.model.FlashVoicingStyleOption { *; }

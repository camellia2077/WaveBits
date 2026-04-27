use crate::util::c_str_to_string;
use crate::{CliError, FlashStyle, TransportMode};
use std::os::raw::{c_char, c_int};
use std::ptr;

type AudioIoWavStatus = c_int;
type AudioIoMetadataStatus = c_int;
type AudioIoMetadataMode = c_int;
type AudioIoMetadataFlashVoicingStyle = c_int;

const AUDIO_IO_WAV_OK: AudioIoWavStatus = 0;
const AUDIO_IO_METADATA_OK: AudioIoMetadataStatus = 0;
const AUDIO_IO_METADATA_MODE_FLASH: AudioIoMetadataMode = 1;
const AUDIO_IO_METADATA_MODE_PRO: AudioIoMetadataMode = 2;
const AUDIO_IO_METADATA_MODE_ULTRA: AudioIoMetadataMode = 3;
const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_CODED_BURST: AudioIoMetadataFlashVoicingStyle = 1;
const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_RITUAL_CHANT: AudioIoMetadataFlashVoicingStyle = 2;
const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_DEEP_RITUAL: AudioIoMetadataFlashVoicingStyle = 3;

#[repr(C)]
struct AudioIoStringView {
    data: *const c_char,
    size: usize,
}

#[repr(C)]
struct AudioIoOwnedString {
    data: *mut c_char,
    size: usize,
}

#[repr(C)]
struct AudioIoByteBuffer {
    data: *mut u8,
    size: usize,
}

#[repr(C)]
struct AudioIoMetadataView {
    version: u8,
    mode: AudioIoMetadataMode,
    has_flash_voicing_style: u8,
    flash_voicing_style: AudioIoMetadataFlashVoicingStyle,
    created_at_iso_utc: AudioIoStringView,
    duration_ms: u32,
    frame_samples: u32,
    pcm_sample_count: u32,
    segment_count: u32,
    // Keep the Rust FFI layout aligned with the native audio_io C ABI even
    // when the CLI itself only writes single-segment metadata today.
    segment_sample_counts: *const u32,
    segment_sample_count_count: usize,
    app_version: AudioIoStringView,
    core_version: AudioIoStringView,
}

#[repr(C)]
struct AudioIoMetadata {
    version: u8,
    mode: AudioIoMetadataMode,
    has_flash_voicing_style: u8,
    flash_voicing_style: AudioIoMetadataFlashVoicingStyle,
    created_at_iso_utc: AudioIoOwnedString,
    duration_ms: u32,
    frame_samples: u32,
    pcm_sample_count: u32,
    segment_count: u32,
    segment_sample_counts: *mut u32,
    segment_sample_count_count: usize,
    app_version: AudioIoOwnedString,
    core_version: AudioIoOwnedString,
}

#[repr(C)]
struct AudioIoDecodedWav {
    sample_rate_hz: i32,
    channels: i32,
    samples: *mut i16,
    sample_count: usize,
    metadata_status: AudioIoMetadataStatus,
    metadata: AudioIoMetadata,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct FlipBitsMetadata {
    pub version: u8,
    pub mode: TransportMode,
    pub flash_voicing_style: Option<FlashStyle>,
    pub created_at_iso_utc: String,
    pub duration_ms: u32,
    pub frame_samples: i32,
    pub pcm_sample_count: usize,
    pub app_version: String,
    pub core_version: String,
}

#[derive(Debug)]
pub struct DecodedWav {
    pub sample_rate_hz: i32,
    pub pcm_samples: Vec<i16>,
    pub metadata: Result<FlipBitsMetadata, String>,
}

unsafe extern "C" {
    fn audio_io_encode_mono_pcm16_wav_with_metadata(
        sample_rate_hz: i32,
        pcm: *const i16,
        sample_count: usize,
        metadata: *const AudioIoMetadataView,
        out_wav_bytes: *mut AudioIoByteBuffer,
    ) -> AudioIoWavStatus;
    fn audio_io_decode_mono_pcm16_wav(
        wav_bytes: *const u8,
        wav_byte_count: usize,
        out_result: *mut AudioIoDecodedWav,
    ) -> AudioIoWavStatus;
    fn audio_io_wav_status_message(status: AudioIoWavStatus) -> *const i8;
    fn audio_io_metadata_status_message(status: AudioIoMetadataStatus) -> *const i8;
    fn audio_io_free_byte_buffer(buffer: *mut AudioIoByteBuffer);
    #[cfg(test)]
    fn audio_io_free_metadata(metadata: *mut AudioIoMetadata);
    fn audio_io_free_decoded_wav(decoded: *mut AudioIoDecodedWav);
}

pub fn encode_mono_pcm16_wav_with_metadata(
    sample_rate_hz: i32,
    pcm_samples: &[i16],
    metadata: &FlipBitsMetadata,
) -> Result<Vec<u8>, CliError> {
    let created_at_bytes = metadata.created_at_iso_utc.as_bytes();
    let app_version_bytes = metadata.app_version.as_bytes();
    let core_version_bytes = metadata.core_version.as_bytes();
    let raw_metadata = AudioIoMetadataView {
        version: metadata.version,
        mode: to_metadata_mode(metadata.mode),
        has_flash_voicing_style: metadata.flash_voicing_style.is_some() as u8,
        flash_voicing_style: metadata
            .flash_voicing_style
            .map(to_flash_voicing_style)
            .unwrap_or(0),
        created_at_iso_utc: AudioIoStringView {
            data: created_at_bytes.as_ptr() as *const c_char,
            size: created_at_bytes.len(),
        },
        duration_ms: metadata.duration_ms,
        frame_samples: metadata.frame_samples as u32,
        pcm_sample_count: metadata.pcm_sample_count as u32,
        segment_count: 1,
        segment_sample_counts: ptr::null(),
        segment_sample_count_count: 0,
        app_version: AudioIoStringView {
            data: app_version_bytes.as_ptr() as *const c_char,
            size: app_version_bytes.len(),
        },
        core_version: AudioIoStringView {
            data: core_version_bytes.as_ptr() as *const c_char,
            size: core_version_bytes.len(),
        },
    };
    let mut out = AudioIoByteBuffer {
        data: ptr::null_mut(),
        size: 0,
    };
    let status = unsafe {
        audio_io_encode_mono_pcm16_wav_with_metadata(
            sample_rate_hz,
            pcm_samples.as_ptr(),
            pcm_samples.len(),
            &raw_metadata,
            &mut out,
        )
    };
    if status != AUDIO_IO_WAV_OK {
        return Err(CliError::Api(c_str_to_string(unsafe {
            audio_io_wav_status_message(status)
        })));
    }

    let bytes = unsafe { std::slice::from_raw_parts(out.data as *const u8, out.size).to_vec() };
    unsafe {
        audio_io_free_byte_buffer(&mut out);
    }
    Ok(bytes)
}

pub fn decode_mono_pcm16_wav(wav_bytes: &[u8]) -> Result<DecodedWav, CliError> {
    let mut out = AudioIoDecodedWav {
        sample_rate_hz: 0,
        channels: 1,
        samples: ptr::null_mut(),
        sample_count: 0,
        metadata_status: 1,
        metadata: AudioIoMetadata {
            version: 0,
            mode: 0,
            has_flash_voicing_style: 0,
            flash_voicing_style: 0,
            created_at_iso_utc: AudioIoOwnedString {
                data: ptr::null_mut(),
                size: 0,
            },
            duration_ms: 0,
            frame_samples: 0,
            pcm_sample_count: 0,
            segment_count: 1,
            segment_sample_counts: ptr::null_mut(),
            segment_sample_count_count: 0,
            app_version: AudioIoOwnedString {
                data: ptr::null_mut(),
                size: 0,
            },
            core_version: AudioIoOwnedString {
                data: ptr::null_mut(),
                size: 0,
            },
        },
    };
    let wav_status =
        unsafe { audio_io_decode_mono_pcm16_wav(wav_bytes.as_ptr(), wav_bytes.len(), &mut out) };
    if wav_status != AUDIO_IO_WAV_OK {
        unsafe {
            audio_io_free_decoded_wav(&mut out);
        }
        return Err(CliError::Api(c_str_to_string(unsafe {
            audio_io_wav_status_message(wav_status)
        })));
    }

    let pcm_samples =
        unsafe { std::slice::from_raw_parts(out.samples as *const i16, out.sample_count).to_vec() };
    let metadata = if out.metadata_status == AUDIO_IO_METADATA_OK {
        let converted = convert_metadata(&out.metadata)?;
        Ok(converted)
    } else {
        Err(c_str_to_string(unsafe {
            audio_io_metadata_status_message(out.metadata_status)
        }))
    };
    let decoded = DecodedWav {
        sample_rate_hz: out.sample_rate_hz,
        pcm_samples,
        metadata,
    };
    unsafe {
        audio_io_free_decoded_wav(&mut out);
    }
    Ok(decoded)
}

#[cfg(test)]
pub fn free_empty_metadata_for_contract_test() {
    let mut metadata = AudioIoMetadata {
        version: 0,
        mode: 0,
        has_flash_voicing_style: 0,
        flash_voicing_style: 0,
        created_at_iso_utc: AudioIoOwnedString {
            data: ptr::null_mut(),
            size: 0,
        },
        duration_ms: 0,
        frame_samples: 0,
        pcm_sample_count: 0,
        segment_count: 1,
        segment_sample_counts: ptr::null_mut(),
        segment_sample_count_count: 0,
        app_version: AudioIoOwnedString {
            data: ptr::null_mut(),
            size: 0,
        },
        core_version: AudioIoOwnedString {
            data: ptr::null_mut(),
            size: 0,
        },
    };
    unsafe {
        audio_io_free_metadata(&mut metadata);
    }
}

fn convert_metadata(raw: &AudioIoMetadata) -> Result<FlipBitsMetadata, CliError> {
    Ok(FlipBitsMetadata {
        version: raw.version,
        mode: from_metadata_mode(raw.mode)?,
        flash_voicing_style: if raw.has_flash_voicing_style != 0 {
            Some(from_flash_voicing_style(raw.flash_voicing_style)?)
        } else {
            None
        },
        created_at_iso_utc: owned_string_to_string(&raw.created_at_iso_utc),
        duration_ms: raw.duration_ms,
        frame_samples: raw.frame_samples as i32,
        pcm_sample_count: raw.pcm_sample_count as usize,
        app_version: owned_string_to_string(&raw.app_version),
        core_version: owned_string_to_string(&raw.core_version),
    })
}

fn owned_string_to_string(raw: &AudioIoOwnedString) -> String {
    if raw.data.is_null() || raw.size == 0 {
        String::new()
    } else {
        let bytes = unsafe { std::slice::from_raw_parts(raw.data as *const u8, raw.size) };
        String::from_utf8_lossy(bytes).into_owned()
    }
}

fn to_metadata_mode(mode: TransportMode) -> AudioIoMetadataMode {
    match mode {
        TransportMode::Flash => AUDIO_IO_METADATA_MODE_FLASH,
        TransportMode::Pro => AUDIO_IO_METADATA_MODE_PRO,
        TransportMode::Ultra => AUDIO_IO_METADATA_MODE_ULTRA,
    }
}

fn from_metadata_mode(mode: AudioIoMetadataMode) -> Result<TransportMode, CliError> {
    match mode {
        AUDIO_IO_METADATA_MODE_FLASH => Ok(TransportMode::Flash),
        AUDIO_IO_METADATA_MODE_PRO => Ok(TransportMode::Pro),
        AUDIO_IO_METADATA_MODE_ULTRA => Ok(TransportMode::Ultra),
        _ => Err(CliError::Api(
            "WAV metadata contained an unknown transport mode".to_string(),
        )),
    }
}

fn to_flash_voicing_style(style: FlashStyle) -> AudioIoMetadataFlashVoicingStyle {
    match style {
        FlashStyle::CodedBurst => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_CODED_BURST,
        FlashStyle::RitualChant => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_RITUAL_CHANT,
        FlashStyle::DeepRitual => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_DEEP_RITUAL,
    }
}

fn from_flash_voicing_style(
    style: AudioIoMetadataFlashVoicingStyle,
) -> Result<FlashStyle, CliError> {
    match style {
        AUDIO_IO_METADATA_FLASH_VOICING_STYLE_CODED_BURST => Ok(FlashStyle::CodedBurst),
        AUDIO_IO_METADATA_FLASH_VOICING_STYLE_RITUAL_CHANT => Ok(FlashStyle::RitualChant),
        AUDIO_IO_METADATA_FLASH_VOICING_STYLE_DEEP_RITUAL => Ok(FlashStyle::DeepRitual),
        _ => Err(CliError::Api(
            "WAV metadata contained an unknown flash voicing style".to_string(),
        )),
    }
}

use crate::util::c_str_to_string;
use crate::{CliError, TransportMode, DEFAULT_FRAME_RATE_DIVISOR, DEFAULT_SAMPLE_RATE_HZ};
use std::ffi::CString;
use std::os::raw::c_int;
use std::ptr;

type BagErrorCode = c_int;
type BagTransportMode = c_int;
type BagFlashSignalProfile = c_int;
type BagFlashVoicingFlavor = c_int;
type BagValidationIssue = c_int;

const BAG_OK: BagErrorCode = 0;
const BAG_TRANSPORT_FLASH: BagTransportMode = 0;
const BAG_TRANSPORT_PRO: BagTransportMode = 1;
const BAG_TRANSPORT_ULTRA: BagTransportMode = 2;
const BAG_FLASH_SIGNAL_PROFILE_CODED_BURST: BagFlashSignalProfile = 0;
const BAG_FLASH_VOICING_FLAVOR_CODED_BURST: BagFlashVoicingFlavor = 0;
const BAG_VALIDATION_OK: BagValidationIssue = 0;

#[repr(C)]
struct BagEncoderConfig {
    sample_rate_hz: i32,
    frame_samples: i32,
    enable_diagnostics: i32,
    mode: BagTransportMode,
    flash_signal_profile: BagFlashSignalProfile,
    flash_voicing_flavor: BagFlashVoicingFlavor,
    reserved: i32,
}

#[repr(C)]
struct BagDecoderConfig {
    sample_rate_hz: i32,
    frame_samples: i32,
    enable_diagnostics: i32,
    mode: BagTransportMode,
    flash_signal_profile: BagFlashSignalProfile,
    flash_voicing_flavor: BagFlashVoicingFlavor,
    reserved: i32,
}

#[repr(C)]
struct BagTextResult {
    buffer: *mut i8,
    buffer_size: usize,
    text_size: usize,
    complete: i32,
    confidence: f32,
    mode: BagTransportMode,
}

#[repr(C)]
struct BagPcm16Result {
    samples: *mut i16,
    sample_count: usize,
}

#[allow(non_camel_case_types)]
enum BagDecoder {}

unsafe extern "C" {
    fn bag_validate_encode_request(config: *const BagEncoderConfig, text: *const i8) -> BagValidationIssue;
    fn bag_validate_decoder_config(config: *const BagDecoderConfig) -> BagValidationIssue;
    fn bag_validation_issue_message(issue: BagValidationIssue) -> *const i8;
    fn bag_error_code_message(code: BagErrorCode) -> *const i8;
    fn bag_encode_text(
        config: *const BagEncoderConfig,
        text: *const i8,
        out_result: *mut BagPcm16Result,
    ) -> BagErrorCode;
    fn bag_free_pcm16_result(result: *mut BagPcm16Result);
    fn bag_create_decoder(config: *const BagDecoderConfig, out_decoder: *mut *mut BagDecoder) -> BagErrorCode;
    fn bag_destroy_decoder(decoder: *mut BagDecoder);
    fn bag_push_pcm(
        decoder: *mut BagDecoder,
        samples: *const i16,
        sample_count: usize,
        timestamp_ms: i64,
    ) -> BagErrorCode;
    fn bag_poll_result(decoder: *mut BagDecoder, out_result: *mut BagTextResult) -> BagErrorCode;
    fn bag_core_version() -> *const i8;
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct CodecConfig {
    pub sample_rate_hz: i32,
    pub frame_samples: i32,
    pub mode: TransportMode,
}

impl CodecConfig {
    pub fn for_mode(mode: TransportMode) -> Self {
        Self {
            sample_rate_hz: DEFAULT_SAMPLE_RATE_HZ,
            frame_samples: default_frame_samples(DEFAULT_SAMPLE_RATE_HZ),
            mode,
        }
    }
}

pub fn core_version() -> Option<String> {
    let raw = unsafe { bag_core_version() };
    if raw.is_null() {
        None
    } else {
        Some(c_str_to_string(raw))
    }
}

pub fn encode_text(config: &CodecConfig, text: &str) -> Result<Vec<i16>, CliError> {
    let c_text = CString::new(text)
        .map_err(|_| CliError::Api("encode text contains an interior NUL byte".to_string()))?;
    let raw_config = make_encoder_config(config);

    let validation = unsafe { bag_validate_encode_request(&raw_config, c_text.as_ptr()) };
    if validation != BAG_VALIDATION_OK {
        return Err(CliError::Api(c_str_to_string(unsafe {
            bag_validation_issue_message(validation)
        })));
    }

    let mut pcm = BagPcm16Result {
        samples: ptr::null_mut(),
        sample_count: 0,
    };
    let code = unsafe { bag_encode_text(&raw_config, c_text.as_ptr(), &mut pcm) };
    if code != BAG_OK {
        return Err(CliError::Api(c_str_to_string(unsafe {
            bag_error_code_message(code)
        })));
    }

    let samples =
        unsafe { std::slice::from_raw_parts(pcm.samples as *const i16, pcm.sample_count).to_vec() };
    unsafe {
        bag_free_pcm16_result(&mut pcm);
    }
    Ok(samples)
}

pub fn decode_pcm(config: &CodecConfig, pcm_samples: &[i16]) -> Result<String, CliError> {
    let raw_config = make_decoder_config(config);
    let validation = unsafe { bag_validate_decoder_config(&raw_config) };
    if validation != BAG_VALIDATION_OK {
        return Err(CliError::Api(c_str_to_string(unsafe {
            bag_validation_issue_message(validation)
        })));
    }

    let mut decoder = ptr::null_mut();
    let create_code = unsafe { bag_create_decoder(&raw_config, &mut decoder) };
    if create_code != BAG_OK || decoder.is_null() {
        return Err(CliError::Api(c_str_to_string(unsafe {
            bag_error_code_message(create_code)
        })));
    }

    let push_code = unsafe { bag_push_pcm(decoder, pcm_samples.as_ptr(), pcm_samples.len(), 0) };
    if push_code != BAG_OK {
        unsafe {
            bag_destroy_decoder(decoder);
        }
        return Err(CliError::Api(c_str_to_string(unsafe {
            bag_error_code_message(push_code)
        })));
    }

    let mut text_buffer = vec![0u8; pcm_samples.len().max(4096)];
    let mut result = BagTextResult {
        buffer: text_buffer.as_mut_ptr() as *mut i8,
        buffer_size: text_buffer.len(),
        text_size: 0,
        complete: 0,
        confidence: 0.0,
        mode: to_bag_mode(config.mode),
    };
    let poll_code = unsafe { bag_poll_result(decoder, &mut result) };
    unsafe {
        bag_destroy_decoder(decoder);
    }
    if poll_code != BAG_OK {
        return Err(CliError::Api(c_str_to_string(unsafe {
            bag_error_code_message(poll_code)
        })));
    }

    String::from_utf8(text_buffer[..result.text_size].to_vec())
        .map_err(|_| CliError::Api("decoded text is not valid UTF-8".to_string()))
}

fn default_frame_samples(sample_rate_hz: i32) -> i32 {
    sample_rate_hz / DEFAULT_FRAME_RATE_DIVISOR
}

fn make_encoder_config(config: &CodecConfig) -> BagEncoderConfig {
    BagEncoderConfig {
        sample_rate_hz: config.sample_rate_hz,
        frame_samples: config.frame_samples,
        enable_diagnostics: 0,
        mode: to_bag_mode(config.mode),
        flash_signal_profile: BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
        flash_voicing_flavor: BAG_FLASH_VOICING_FLAVOR_CODED_BURST,
        reserved: 0,
    }
}

fn make_decoder_config(config: &CodecConfig) -> BagDecoderConfig {
    BagDecoderConfig {
        sample_rate_hz: config.sample_rate_hz,
        frame_samples: config.frame_samples,
        enable_diagnostics: 0,
        mode: to_bag_mode(config.mode),
        flash_signal_profile: BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
        flash_voicing_flavor: BAG_FLASH_VOICING_FLAVOR_CODED_BURST,
        reserved: 0,
    }
}

fn to_bag_mode(mode: TransportMode) -> BagTransportMode {
    match mode {
        TransportMode::Flash => BAG_TRANSPORT_FLASH,
        TransportMode::Pro => BAG_TRANSPORT_PRO,
        TransportMode::Ultra => BAG_TRANSPORT_ULTRA,
    }
}

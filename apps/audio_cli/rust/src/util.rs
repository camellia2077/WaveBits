use std::ffi::CStr;
use std::os::raw::c_char;

pub fn c_str_to_string(raw: *const c_char) -> String {
    if raw.is_null() {
        "unknown".to_string()
    } else {
        unsafe { CStr::from_ptr(raw) }
            .to_string_lossy()
            .into_owned()
    }
}

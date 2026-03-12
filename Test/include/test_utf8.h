#pragma once

#if !defined(WAVEBITS_TEST_IMPORT_STD)
#include <string>
#endif

namespace test {

inline std::string Utf8Literal(const char* text) {
    return std::string(text);
}

#if defined(__cpp_char8_t)
inline std::string Utf8Literal(const char8_t* text) {
    const auto* bytes = reinterpret_cast<const char*>(text);
    std::size_t length = 0;
    while (text[length] != u8'\0') {
        ++length;
    }
    return std::string(bytes, length);
}
#endif

}  // namespace test

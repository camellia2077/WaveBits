#pragma once

#if !defined(FLIPBITS_TEST_IMPORT_STD)
#include <atomic>
#include <chrono>
#include <filesystem>
#include <fstream>
#include <stdexcept>
#include <string>
#endif

namespace test {

inline std::filesystem::path MakeTempDir(const std::string& suite_name) {
    const auto root = std::filesystem::temp_directory_path() / "binary_audio_generator_tests";
    const auto suite_root = root / suite_name;
    std::filesystem::create_directories(suite_root);

    static const auto run_prefix = [] {
        const auto now = std::chrono::system_clock::now().time_since_epoch();
        const auto millis = std::chrono::duration_cast<std::chrono::milliseconds>(now).count();
        return std::to_string(millis);
    }();
    static std::atomic<unsigned long long> next_id{0};

    for (;;) {
        const auto dir = suite_root / (run_prefix + "-" + std::to_string(next_id.fetch_add(1)));
        if (std::filesystem::create_directory(dir)) {
            return dir;
        }
    }
}

inline void WriteTextFile(const std::filesystem::path& path, const std::string& content) {
    const auto parent = path.parent_path();
    if (!parent.empty()) {
        std::filesystem::create_directories(parent);
    }

    std::ofstream file(path, std::ios::binary);
    if (!file) {
        throw std::runtime_error("Failed to write file: " + path.string());
    }
    file << content;
}

inline std::string ReadTextFile(const std::filesystem::path& path) {
    std::ifstream file(path, std::ios::binary);
    if (!file) {
        throw std::runtime_error("Failed to read file: " + path.string());
    }
    return std::string((std::istreambuf_iterator<char>(file)), std::istreambuf_iterator<char>());
}

}  // namespace test

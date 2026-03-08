#include <filesystem>
#include <fstream>
#include <iostream>
#include <stdexcept>
#include <string>
#include <vector>

#include "bag_api.h"
#include "wav_io.h"

namespace {

constexpr char kCliPresentationVersion[] = "0.1.1";
constexpr int kDefaultSampleRateHz = 44100;

int default_frame_samples(int sample_rate_hz) {
    return sample_rate_hz / 20;
}

bag_encoder_config make_encoder_config(bag_transport_mode mode) {
    bag_encoder_config config{};
    config.sample_rate_hz = kDefaultSampleRateHz;
    config.frame_samples = default_frame_samples(config.sample_rate_hz);
    config.enable_diagnostics = 0;
    config.mode = mode;
    config.reserved = 0;
    return config;
}

bag_decoder_config make_decoder_config(int sample_rate_hz, bag_transport_mode mode) {
    bag_decoder_config config{};
    config.sample_rate_hz = sample_rate_hz > 0 ? sample_rate_hz : kDefaultSampleRateHz;
    config.frame_samples = default_frame_samples(config.sample_rate_hz);
    config.enable_diagnostics = 0;
    config.mode = mode;
    config.reserved = 0;
    return config;
}

struct pcm_result_guard {
    bag_pcm16_result result{};

    ~pcm_result_guard() {
        bag_free_pcm16_result(&result);
    }
};

void print_versions() {
    const char* version = bag_core_version();
    std::cout << "presentation: v" << kCliPresentationVersion << "\n"
              << "core: v" << (version != nullptr ? version : "unknown") << "\n";
}

void print_usage() {
    std::cout << "Usage:\n"
              << "  binary_audio_cpp encode --text <TEXT> --out <OUTPUT.wav> [--mode flash|pro|ultra]\n"
              << "  binary_audio_cpp encode --text-file <FILE> --out <OUTPUT.wav> [--mode flash|pro|ultra]\n"
              << "  binary_audio_cpp decode --in <INPUT.wav> [--out-text <OUTPUT.txt>] [--mode flash|pro|ultra]\n"
              << "  binary_audio_cpp version\n"
              << "  binary_audio_cpp --version\n";
}

std::string read_text_file(const std::filesystem::path& path) {
    std::ifstream file(path, std::ios::binary);
    if (!file) {
        throw std::runtime_error("Failed to open text file: " + path.string());
    }
    return std::string((std::istreambuf_iterator<char>(file)), std::istreambuf_iterator<char>());
}

std::filesystem::path normalize_output_path(const std::string& raw,
                                            const std::string& default_name,
                                            const std::string& extension) {
    std::filesystem::path path(raw);
    const bool ends_with_sep =
        !raw.empty() && (raw.back() == '/' || raw.back() == '\\');

    if (std::filesystem::exists(path) && std::filesystem::is_directory(path)) {
        path /= default_name;
    } else if (ends_with_sep) {
        path /= default_name;
    } else if (!path.has_extension()) {
        path.replace_extension(extension);
    }
    return path;
}

void ensure_parent_directory(const std::filesystem::path& path) {
    const auto parent = path.parent_path();
    if (!parent.empty()) {
        std::filesystem::create_directories(parent);
    }
}

}  // namespace

int main(int argc, char* argv[]) {
    if (argc < 2) {
        print_usage();
        return 1;
    }

    const std::string command = argv[1];
    if (command == "version" || command == "--version") {
        print_versions();
        return 0;
    }

    try {
        if (command == "encode") {
            std::string text;
            std::string text_file;
            std::string out = "data/output_audio/output.wav";
            bag_transport_mode mode = BAG_TRANSPORT_FLASH;

            for (int index = 2; index < argc; ++index) {
                const std::string arg = argv[index];
                if (arg == "--text" && index + 1 < argc) {
                    text = argv[++index];
                } else if (arg == "--text-file" && index + 1 < argc) {
                    text_file = argv[++index];
                } else if (arg == "--out" && index + 1 < argc) {
                    out = argv[++index];
                } else if (arg == "--mode" && index + 1 < argc) {
                    const std::string raw_mode = argv[++index];
                    if (!bag_try_parse_transport_mode(raw_mode.c_str(), &mode)) {
                        throw std::runtime_error(bag_validation_issue_message(BAG_VALIDATION_INVALID_MODE));
                    }
                } else {
                    print_usage();
                    return 1;
                }
            }

            if ((!text.empty()) == (!text_file.empty())) {
                std::cerr << "Please provide exactly one of --text or --text-file.\n";
                return 1;
            }

            if (!text_file.empty()) {
                text = read_text_file(text_file);
            }

            const auto output_path = normalize_output_path(out, "output.wav", ".wav");
            const bag_encoder_config config = make_encoder_config(mode);
            const bag_validation_issue validation_issue =
                bag_validate_encode_request(&config, text.c_str());
            if (validation_issue != BAG_VALIDATION_OK) {
                throw std::runtime_error(bag_validation_issue_message(validation_issue));
            }
            pcm_result_guard pcm{};
            const bag_error_code encode_code = bag_encode_text(&config, text.c_str(), &pcm.result);
            if (encode_code != BAG_OK) {
                throw std::runtime_error(
                    "Encode failed in `" + std::string(bag_transport_mode_name(mode)) +
                    "` mode: " + bag_error_code_message(encode_code));
            }

            const std::vector<int16_t> mono_pcm =
                pcm.result.sample_count > 0
                    ? std::vector<int16_t>(
                          pcm.result.samples,
                          pcm.result.samples + pcm.result.sample_count)
                    : std::vector<int16_t>{};
            ensure_parent_directory(output_path);
            audio_io::WriteMonoPcm16Wav(output_path, config.sample_rate_hz, mono_pcm);
            std::cout << "Mode: " << bag_transport_mode_name(mode) << "\n";
            std::cout << "Output WAV: " << output_path.string() << "\n";
        } else if (command == "decode") {
            std::string input;
            std::string out_text;
            bag_transport_mode mode = BAG_TRANSPORT_FLASH;

            for (int index = 2; index < argc; ++index) {
                const std::string arg = argv[index];
                if (arg == "--in" && index + 1 < argc) {
                    input = argv[++index];
                } else if (arg == "--out-text" && index + 1 < argc) {
                    out_text = argv[++index];
                } else if (arg == "--mode" && index + 1 < argc) {
                    const std::string raw_mode = argv[++index];
                    if (!bag_try_parse_transport_mode(raw_mode.c_str(), &mode)) {
                        throw std::runtime_error(bag_validation_issue_message(BAG_VALIDATION_INVALID_MODE));
                    }
                } else {
                    print_usage();
                    return 1;
                }
            }

            if (input.empty()) {
                print_usage();
                return 1;
            }

            const auto wav = audio_io::ReadMonoPcm16Wav(input);
            const bag_decoder_config config = make_decoder_config(wav.sample_rate_hz, mode);
            const bag_validation_issue validation_issue = bag_validate_decoder_config(&config);
            if (validation_issue != BAG_VALIDATION_OK) {
                throw std::runtime_error(bag_validation_issue_message(validation_issue));
            }
            bag_decoder* decoder = nullptr;
            const bag_error_code create_code = bag_create_decoder(&config, &decoder);
            if (create_code != BAG_OK || decoder == nullptr) {
                throw std::runtime_error(
                    "Decode setup failed in `" + std::string(bag_transport_mode_name(mode)) +
                    "` mode: " + bag_error_code_message(create_code));
            }

            const bag_error_code push_code =
                bag_push_pcm(decoder, wav.mono_pcm.data(), wav.mono_pcm.size(), 0);
            if (push_code != BAG_OK) {
                bag_destroy_decoder(decoder);
                throw std::runtime_error(
                    "Decode input failed in `" + std::string(bag_transport_mode_name(mode)) +
                    "` mode: " + bag_error_code_message(push_code));
            }

            std::vector<char> text_buffer(4096, '\0');
            bag_text_result result{};
            result.buffer = text_buffer.data();
            result.buffer_size = text_buffer.size();

            const bag_error_code poll_code = bag_poll_result(decoder, &result);
            bag_destroy_decoder(decoder);
            if (poll_code != BAG_OK) {
                throw std::runtime_error(
                    "Decode failed in `" + std::string(bag_transport_mode_name(mode)) +
                    "` mode: " + bag_error_code_message(poll_code));
            }

            const std::string decoded(text_buffer.data(), result.text_size);
            if (!out_text.empty()) {
                const auto output_path = normalize_output_path(out_text, "decoded.txt", ".txt");
                ensure_parent_directory(output_path);
                std::ofstream out_file(output_path, std::ios::binary);
                if (!out_file) {
                    throw std::runtime_error("Failed to write output text file.");
                }
                out_file << decoded;
            }
            std::cout << decoded << "\n";
        } else {
            print_usage();
            return 1;
        }
    } catch (const std::exception& ex) {
        std::cerr << "Error: " << ex.what() << "\n";
        return 1;
    }

    return 0;
}

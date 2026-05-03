#include "test_std_support.h"
#include "test_audio_io.h"
#include "test_fs.h"
#include "test_framework.h"
#include "test_vectors.h"
#include "test_utf8.h"

import audio_io.wav;
import bag.common.error_code;
import bag.common.version;
import bag.flash.codec;
import bag.flash.signal;
import bag.flash.voicing;
import bag.flash.phy_clean;
import bag.pro.codec;
import bag.transport.compat.frame_codec;
import bag.ultra.codec;

#include "leaf_module_smoke_support.h"

namespace modules_leaf_smoke {

bag::flash::BfskConfig MakeBfskConfig() {
    bag::flash::BfskConfig config{};
    config.sample_rate_hz = 44100;
    config.samples_per_bit = 2205;
    config.bit_duration_sec = 0.05;
    return config;
}

bag::flash::FlashVoicingConfig MakeStyledVoicingConfig() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    config.attack_ratio = 0.08;
    config.release_ratio = 0.08;
    config.second_harmonic_gain = 0.10;
    config.third_harmonic_gain = 0.03;
    config.boundary_click_gain = 0.02;
    return config;
}

bag::CoreConfig MakeFlashCoreConfig() {
    bag::CoreConfig config{};
    config.sample_rate_hz = 44100;
    config.frame_samples = 2205;
    config.mode = bag::TransportMode::kFlash;
    return config;
}

bag::CoreConfig MakeLitanyFlashCoreConfig() {
    auto config = MakeFlashCoreConfig();
    config.flash_signal_profile = bag::FlashSignalProfile::kLitany;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kLitany;
    return config;
}

bag::CoreConfig MakeHostileFlashCoreConfig() {
    auto config = MakeFlashCoreConfig();
    config.flash_signal_profile = bag::FlashSignalProfile::kHostile;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kHostile;
    return config;
}

bag::CoreConfig MakeCollapseFlashCoreConfig() {
    auto config = MakeFlashCoreConfig();
    config.flash_signal_profile = bag::FlashSignalProfile::kCollapse;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kCollapse;
    return config;
}

bag::CoreConfig MakeZealFlashCoreConfig() {
    auto config = MakeFlashCoreConfig();
    config.flash_signal_profile = bag::FlashSignalProfile::kZeal;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kZeal;
    return config;
}

bag::CoreConfig MakeExplicitDecoupledFlashCoreConfig() {
    auto config = MakeFlashCoreConfig();
    config.flash_signal_profile = bag::FlashSignalProfile::kSteady;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kLitany;
    return config;
}

std::size_t FormalFlashLeadingSamples(const bag::CoreConfig& config) {
    return config.frame_samples > 0
               ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(3)
               : static_cast<std::size_t>(0);
}

std::size_t FormalFlashTrailingSamples(const bag::CoreConfig& config) {
    return config.frame_samples > 0
               ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(3)
               : static_cast<std::size_t>(0);
}

std::vector<std::uint8_t> AsBytes(const std::string& text) {
    return std::vector<std::uint8_t>(text.begin(), text.end());
}

}  // namespace modules_leaf_smoke

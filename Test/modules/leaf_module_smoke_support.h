#pragma once

namespace test {
class Runner;
}

namespace modules_leaf_smoke {

bag::flash::BfskConfig MakeBfskConfig();
bag::flash::FlashVoicingConfig MakeStyledVoicingConfig();
bag::CoreConfig MakeFlashCoreConfig();
bag::CoreConfig MakeRitualFlashCoreConfig();
bag::CoreConfig MakeExplicitDecoupledFlashCoreConfig();

std::size_t FormalFlashLeadingSamples(const bag::CoreConfig& config);
std::size_t FormalFlashTrailingSamples(const bag::CoreConfig& config);
std::vector<std::uint8_t> AsBytes(const std::string& text);

void RegisterLeafAudioIoTests(test::Runner& runner);
void RegisterLeafFlashTests(test::Runner& runner);
void RegisterLeafTransportTests(test::Runner& runner);

}  // namespace modules_leaf_smoke

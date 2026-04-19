#pragma once

namespace test {
class Runner;
}

namespace flash_voicing_test {

bag::flash::BfskConfig MakeSignalConfig();
bag::CoreConfig MakeCoreConfig();
bag::CoreConfig MakeAndroidSizedCoreConfig();

std::size_t FormalPreambleSampleCountForFlavor(const bag::CoreConfig& config,
                                               bag::FlashVoicingFlavor flavor);
std::size_t FormalEpilogueSampleCountForFlavor(const bag::CoreConfig& config,
                                               bag::FlashVoicingFlavor flavor);

bag::flash::FlashVoicingConfig MakeEnvelopeOnlyConfig();
bag::flash::FlashVoicingConfig MakeHarmonicOnlyConfig();
bag::flash::FlashVoicingConfig MakeClickOnlyConfig();
bag::flash::FlashVoicingConfig MakeStyledConfig();
bag::flash::FlashVoicingConfig MakeStyledShellConfig();
bag::flash::FlashVoicingConfig MakeTrimEnabledConfig(std::size_t preamble_sample_count,
                                                     std::size_t epilogue_sample_count);

std::vector<std::uint8_t> AsBytes(const std::string& text);
bag::flash::FlashPayloadLayout MakePayloadLayout(const std::string& text);
std::vector<std::int16_t> MakeCleanPayload(const std::string& text);

void AssertPcm16Range(const std::vector<std::int16_t>& pcm, const std::string& context);
double AverageAbsoluteSample(const std::vector<std::int16_t>& pcm,
                             std::size_t begin,
                             std::size_t end);
double AverageAbsoluteDelta(const std::vector<std::int16_t>& first,
                            const std::vector<std::int16_t>& second,
                            std::size_t begin,
                            std::size_t end);
double AverageAbsoluteRangeDelta(const std::vector<std::int16_t>& pcm,
                                 std::size_t first_begin,
                                 std::size_t first_end,
                                 std::size_t second_begin,
                                 std::size_t second_end);
double AverageNormalizedFirstDifference(const std::vector<std::int16_t>& pcm,
                                        std::size_t begin,
                                        std::size_t end);
std::pair<std::size_t, std::size_t> FractionalRange(std::size_t sample_count,
                                                    double begin_ratio,
                                                    double end_ratio);

void RegisterFlashVoicingPayloadTests(test::Runner& runner);
void RegisterFlashVoicingFormalTests(test::Runner& runner);
void RegisterFlashVoicingShellTests(test::Runner& runner);

}  // namespace flash_voicing_test

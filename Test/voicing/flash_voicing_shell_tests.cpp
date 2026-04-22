#include "test_std_support.h"
#include "test_framework.h"

import bag.flash.phy_clean;
import bag.flash.signal;
import bag.flash.voicing;

#include "flash_voicing_test_support.h"

namespace {

using namespace flash_voicing_test;

void TestRitualChantPreambleUsesThreePhrasedBursts() {
    constexpr std::size_t kPreambleSampleCount = 3000;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kRitualChant,
        MakeTrimEnabledConfig(kPreambleSampleCount, static_cast<std::size_t>(0)));

    const auto [phrase1_begin, phrase1_end] = FractionalRange(kPreambleSampleCount, 0.08, 0.18);
    const auto [gap1_begin, gap1_end] = FractionalRange(kPreambleSampleCount, 0.25, 0.29);
    const auto [phrase2_begin, phrase2_end] = FractionalRange(kPreambleSampleCount, 0.40, 0.50);
    const auto [gap2_begin, gap2_end] = FractionalRange(kPreambleSampleCount, 0.61, 0.65);
    const auto [phrase3_begin, phrase3_end] = FractionalRange(kPreambleSampleCount, 0.76, 0.86);

    const double phrase1_energy = AverageAbsoluteSample(voiced.pcm, phrase1_begin, phrase1_end);
    const double gap1_energy = AverageAbsoluteSample(voiced.pcm, gap1_begin, gap1_end);
    const double phrase2_energy = AverageAbsoluteSample(voiced.pcm, phrase2_begin, phrase2_end);
    const double gap2_energy = AverageAbsoluteSample(voiced.pcm, gap2_begin, gap2_end);
    const double phrase3_energy = AverageAbsoluteSample(voiced.pcm, phrase3_begin, phrase3_end);

    test::AssertTrue(
        phrase1_energy > gap1_energy * 3.0,
        "ritual_chant preamble should leave a clear short pause between phrase one and phrase two.");
    test::AssertTrue(
        phrase2_energy > gap1_energy * 3.0,
        "ritual_chant preamble should re-enter strongly after the first short pause.");
    test::AssertTrue(
        phrase2_energy > gap2_energy * 3.0,
        "ritual_chant preamble should leave a second clear short pause before phrase three.");
    test::AssertTrue(
        phrase3_energy > gap2_energy * 3.0,
        "ritual_chant preamble should end with a third voiced phrase after the second short pause.");
}

void TestRitualChantEpilogueUsesTwoPhrasedBursts() {
    constexpr std::size_t kEpilogueSampleCount = 1800;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kRitualChant,
        MakeTrimEnabledConfig(static_cast<std::size_t>(0), kEpilogueSampleCount));

    const std::size_t epilogue_begin = voiced.descriptor.leading_nonpayload_samples +
                                       voiced.descriptor.payload_sample_count;
    const auto [phrase1_begin_local, phrase1_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.16, 0.32);
    const auto [gap_begin_local, gap_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.48, 0.54);
    const auto [phrase2_begin_local, phrase2_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.70, 0.84);

    const double phrase1_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + phrase1_begin_local,
        epilogue_begin + phrase1_end_local);
    const double gap_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + gap_begin_local,
        epilogue_begin + gap_end_local);
    const double phrase2_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + phrase2_begin_local,
        epilogue_begin + phrase2_end_local);

    test::AssertTrue(
        phrase1_energy > gap_energy * 3.0,
        "ritual_chant epilogue should insert a short pause after the first closure phrase.");
    test::AssertTrue(
        phrase2_energy > gap_energy * 3.0,
        "ritual_chant epilogue should resume with a second closure phrase after the short pause.");
}

void TestDeepRitualPreambleUsesWiderThreePhrasedBursts() {
    constexpr std::size_t kPreambleSampleCount = 4200;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kDeepRitual,
        MakeTrimEnabledConfig(kPreambleSampleCount, static_cast<std::size_t>(0)));

    const auto [phrase1_begin, phrase1_end] = FractionalRange(kPreambleSampleCount, 0.07, 0.19);
    const auto [gap1_begin, gap1_end] = FractionalRange(kPreambleSampleCount, 0.23, 0.29);
    const auto [phrase2_begin, phrase2_end] = FractionalRange(kPreambleSampleCount, 0.36, 0.52);
    const auto [gap2_begin, gap2_end] = FractionalRange(kPreambleSampleCount, 0.57, 0.64);
    const auto [phrase3_begin, phrase3_end] = FractionalRange(kPreambleSampleCount, 0.72, 0.90);

    const double phrase1_energy = AverageAbsoluteSample(voiced.pcm, phrase1_begin, phrase1_end);
    const double gap1_energy = AverageAbsoluteSample(voiced.pcm, gap1_begin, gap1_end);
    const double phrase2_energy = AverageAbsoluteSample(voiced.pcm, phrase2_begin, phrase2_end);
    const double gap2_energy = AverageAbsoluteSample(voiced.pcm, gap2_begin, gap2_end);
    const double phrase3_energy = AverageAbsoluteSample(voiced.pcm, phrase3_begin, phrase3_end);

    test::AssertTrue(
        phrase1_energy > gap1_energy * 3.0,
        "deep_ritual preamble should leave a broad pause after the first phrase.");
    test::AssertTrue(
        phrase2_energy > gap1_energy * 3.0,
        "deep_ritual preamble should re-enter strongly after the first broad pause.");
    test::AssertTrue(
        phrase2_energy > gap2_energy * 3.0,
        "deep_ritual preamble should leave a second broad pause before the final phrase.");
    test::AssertTrue(
        phrase3_energy > gap2_energy * 3.0,
        "deep_ritual preamble should finish with a third elongated voiced phrase.");
}

void TestCodedBurstPreambleUsesThreeHandshakeBursts() {
    constexpr std::size_t kPreambleSampleCount = 1600;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kCodedBurst,
        MakeTrimEnabledConfig(kPreambleSampleCount, static_cast<std::size_t>(0)));

    const auto [burst1_begin, burst1_end] = FractionalRange(kPreambleSampleCount, 0.05, 0.18);
    const auto [gap1_begin, gap1_end] = FractionalRange(kPreambleSampleCount, 0.21, 0.27);
    const auto [burst2_begin, burst2_end] = FractionalRange(kPreambleSampleCount, 0.30, 0.43);
    const auto [gap2_begin, gap2_end] = FractionalRange(kPreambleSampleCount, 0.47, 0.54);
    const auto [burst3_begin, burst3_end] = FractionalRange(kPreambleSampleCount, 0.58, 0.70);

    const double burst1_energy = AverageAbsoluteSample(voiced.pcm, burst1_begin, burst1_end);
    const double gap1_energy = AverageAbsoluteSample(voiced.pcm, gap1_begin, gap1_end);
    const double burst2_energy = AverageAbsoluteSample(voiced.pcm, burst2_begin, burst2_end);
    const double gap2_energy = AverageAbsoluteSample(voiced.pcm, gap2_begin, gap2_end);
    const double burst3_energy = AverageAbsoluteSample(voiced.pcm, burst3_begin, burst3_end);

    test::AssertTrue(
        burst1_energy > gap1_energy * 3.0,
        "coded_burst preamble should open with a strong short handshake burst.");
    test::AssertTrue(
        burst2_energy > gap1_energy * 3.0,
        "coded_burst preamble should re-enter with a short confirmation burst after the first gap.");
    test::AssertTrue(
        burst2_energy > gap2_energy * 3.0,
        "coded_burst preamble should leave a clear second gap before the sync burst.");
    test::AssertTrue(
        burst3_energy > gap2_energy * 3.0,
        "coded_burst preamble should finish with a third sync burst that pushes directly into payload onset.");
}

void TestCodedBurstPreambleContrastsPayloadOnset() {
    constexpr std::size_t kPreambleSampleCount = 1600;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kCodedBurst,
        MakeTrimEnabledConfig(kPreambleSampleCount, static_cast<std::size_t>(0)));

    const auto [sync_begin, sync_end] = FractionalRange(kPreambleSampleCount, 0.58, 0.70);
    const std::size_t sync_length = sync_end - sync_begin;
    const std::size_t payload_begin = voiced.descriptor.leading_nonpayload_samples;
    const std::size_t payload_end = payload_begin + sync_length;
    const auto [seam_begin, seam_end] = FractionalRange(kPreambleSampleCount, 0.78, 0.94);

    const double sync_delta = AverageAbsoluteRangeDelta(
        voiced.pcm,
        sync_begin,
        sync_end,
        payload_begin,
        payload_end);
    const double sync_brightness = AverageNormalizedFirstDifference(voiced.pcm, sync_begin, sync_end);
    const double payload_brightness =
        AverageNormalizedFirstDifference(voiced.pcm, payload_begin, payload_end);
    const double sync_energy = AverageAbsoluteSample(voiced.pcm, sync_begin, sync_end);
    const double seam_energy = AverageAbsoluteSample(voiced.pcm, seam_begin, seam_end);

    test::AssertTrue(
        sync_delta > 5500.0,
        "coded_burst preamble sync burst should not sound like a continuation of payload onset.");
    test::AssertTrue(
        sync_brightness > payload_brightness * 1.08,
        "coded_burst preamble sync burst should sound brighter than the payload start.");
    test::AssertTrue(
        seam_energy < sync_energy * 0.18,
        "coded_burst preamble should leave a low-energy seam before payload begins.");
}

void TestCodedBurstEpilogueUsesClosingBurstAndAckChirp() {
    constexpr std::size_t kEpilogueSampleCount = 1200;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kCodedBurst,
        MakeTrimEnabledConfig(static_cast<std::size_t>(0), kEpilogueSampleCount));

    const std::size_t epilogue_begin = voiced.descriptor.leading_nonpayload_samples +
                                       voiced.descriptor.payload_sample_count;
    const auto [burst_begin_local, burst_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.12, 0.32);
    const auto [gap_begin_local, gap_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.40, 0.50);
    const auto [ack_begin_local, ack_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.54, 0.68);
    const auto [tail_begin_local, tail_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.82, 0.96);

    const double burst_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + burst_begin_local,
        epilogue_begin + burst_end_local);
    const double gap_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + gap_begin_local,
        epilogue_begin + gap_end_local);
    const double ack_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + ack_begin_local,
        epilogue_begin + ack_end_local);
    const double tail_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + tail_begin_local,
        epilogue_begin + tail_end_local);

    test::AssertTrue(
        burst_energy > gap_energy * 3.0,
        "coded_burst epilogue should begin with a short closing burst before the acknowledgement gap.");
    test::AssertTrue(
        ack_energy > gap_energy * 3.0,
        "coded_burst epilogue should emit a distinct ack chirp after the main closing burst.");
    test::AssertTrue(
        tail_energy < ack_energy * 0.45,
        "coded_burst epilogue should decay quickly after the ack chirp instead of trailing like ritual_chant.");
}

void TestCodedBurstEpilogueContrastsPayloadTailAndStopsHard() {
    constexpr std::size_t kPreambleSampleCount = 1600;
    constexpr std::size_t kEpilogueSampleCount = 1200;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto preamble_voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kCodedBurst,
        MakeTrimEnabledConfig(kPreambleSampleCount, static_cast<std::size_t>(0)));
    const auto epilogue_voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kCodedBurst,
        MakeTrimEnabledConfig(static_cast<std::size_t>(0), kEpilogueSampleCount));

    const auto [sync_begin, sync_end] = FractionalRange(kPreambleSampleCount, 0.58, 0.70);
    const double preamble_sync_brightness =
        AverageNormalizedFirstDifference(preamble_voiced.pcm, sync_begin, sync_end);

    const std::size_t epilogue_begin = epilogue_voiced.descriptor.leading_nonpayload_samples +
                                       epilogue_voiced.descriptor.payload_sample_count;
    const auto [closing_begin_local, closing_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.12, 0.32);
    const auto [ack_begin_local, ack_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.54, 0.68);
    const auto [tail_begin_local, tail_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.82, 0.96);

    const std::size_t closing_length = closing_end_local - closing_begin_local;
    const std::size_t payload_tail_end = epilogue_begin;
    const std::size_t payload_tail_begin = payload_tail_end - closing_length;

    const double closing_delta = AverageAbsoluteRangeDelta(
        epilogue_voiced.pcm,
        epilogue_begin + closing_begin_local,
        epilogue_begin + closing_end_local,
        payload_tail_begin,
        payload_tail_end);
    const double ack_brightness = AverageNormalizedFirstDifference(
        epilogue_voiced.pcm,
        epilogue_begin + ack_begin_local,
        epilogue_begin + ack_end_local);
    const double ack_energy = AverageAbsoluteSample(
        epilogue_voiced.pcm,
        epilogue_begin + ack_begin_local,
        epilogue_begin + ack_end_local);
    const double tail_energy = AverageAbsoluteSample(
        epilogue_voiced.pcm,
        epilogue_begin + tail_begin_local,
        epilogue_begin + tail_end_local);

    test::AssertTrue(
        closing_delta > 4200.0,
        "coded_burst closing burst should not sound like an extension of the payload tail.");
    test::AssertTrue(
        ack_brightness < preamble_sync_brightness * 0.92,
        "coded_burst ack chirp should sound lower and less bright than the preamble sync burst.");
    test::AssertTrue(
        tail_energy < ack_energy * 0.12,
        "coded_burst epilogue should hard-stop after the ack chirp.");
}

void TestTrimDescriptorTracksNonpayloadSamples() {
    const auto descriptor = bag::flash::DescribeVoicingOutput(
        static_cast<std::size_t>(24),
        MakeTrimEnabledConfig(static_cast<std::size_t>(5), static_cast<std::size_t>(7)));

    test::AssertEq(
        descriptor.leading_nonpayload_samples,
        static_cast<std::size_t>(5),
        "Trim descriptor should report configured leading non-payload samples.");
    test::AssertEq(
        descriptor.trailing_nonpayload_samples,
        static_cast<std::size_t>(7),
        "Trim descriptor should report configured trailing non-payload samples.");
    test::AssertEq(
        descriptor.payload_sample_count,
        static_cast<std::size_t>(12),
        "Trim descriptor should report the remaining payload sample count.");
}

void TestTrimToPayloadPcmExtractsPayloadForDecode() {
    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayload(
        clean_payload,
        layout,
        MakeTrimEnabledConfig(static_cast<std::size_t>(6), static_cast<std::size_t>(4)));
    const auto trimmed_payload = bag::flash::TrimToPayloadPcm(voiced.pcm, voiced.descriptor);

    test::AssertEq(
        trimmed_payload,
        clean_payload,
        "Trim helper should return the original payload PCM after removing non-payload samples.");
    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(trimmed_payload, MakeSignalConfig()),
        AsBytes("AB"),
        "Trimmed payload should remain decodable through the flash signal layer.");
}

void TestPreambleAndEpilogueSegmentsAreInserted() {
    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayload(
        clean_payload,
        layout,
        MakeTrimEnabledConfig(static_cast<std::size_t>(6), static_cast<std::size_t>(4)));

    test::AssertEq(
        voiced.descriptor.leading_nonpayload_samples,
        static_cast<std::size_t>(6),
        "Voicing should report inserted preamble sample count.");
    test::AssertEq(
        voiced.descriptor.trailing_nonpayload_samples,
        static_cast<std::size_t>(4),
        "Voicing should report inserted epilogue sample count.");
    test::AssertEq(
        voiced.pcm.size(),
        clean_payload.size() + static_cast<std::size_t>(10),
        "Voicing should grow the PCM length by preamble plus epilogue sample counts.");

    bool has_nonzero_preamble = false;
    for (std::size_t index = 0; index < voiced.descriptor.leading_nonpayload_samples; ++index) {
        has_nonzero_preamble = has_nonzero_preamble || voiced.pcm[index] != 0;
    }

    bool has_nonzero_epilogue = false;
    const std::size_t epilogue_begin =
        voiced.descriptor.leading_nonpayload_samples + voiced.descriptor.payload_sample_count;
    for (std::size_t index = epilogue_begin; index < voiced.pcm.size(); ++index) {
        has_nonzero_epilogue = has_nonzero_epilogue || voiced.pcm[index] != 0;
    }

    test::AssertTrue(has_nonzero_preamble, "Inserted preamble should contain audible non-zero samples.");
    test::AssertTrue(has_nonzero_epilogue, "Inserted epilogue should contain audible non-zero samples.");
}

void TestTrimRejectsDescriptorMismatch() {
    const std::vector<std::int16_t> pcm = {1, 2, 3, 4, 5, 6};
    bag::flash::FlashVoicingDescriptor descriptor{};
    descriptor.leading_nonpayload_samples = 2;
    descriptor.payload_sample_count = 3;
    descriptor.trailing_nonpayload_samples = 2;

    test::AssertThrows(
        [&] {
            (void)bag::flash::TrimToPayloadPcm(pcm, descriptor);
        },
        "Trim helper should reject descriptors whose total sample count does not match the PCM.");
}

}  // namespace

namespace flash_voicing_test {

void RegisterFlashVoicingShellTests(test::Runner& runner) {
    runner.Add("FlashVoicing.RitualChantPreambleUsesThreePhrasedBursts",
               TestRitualChantPreambleUsesThreePhrasedBursts);
    runner.Add("FlashVoicing.RitualChantEpilogueUsesTwoPhrasedBursts",
               TestRitualChantEpilogueUsesTwoPhrasedBursts);
    runner.Add("FlashVoicing.DeepRitualPreambleUsesWiderThreePhrasedBursts",
               TestDeepRitualPreambleUsesWiderThreePhrasedBursts);
    runner.Add("FlashVoicing.CodedBurstPreambleUsesThreeHandshakeBursts",
               TestCodedBurstPreambleUsesThreeHandshakeBursts);
    runner.Add("FlashVoicing.CodedBurstPreambleContrastsPayloadOnset",
               TestCodedBurstPreambleContrastsPayloadOnset);
    runner.Add("FlashVoicing.CodedBurstEpilogueUsesClosingBurstAndAckChirp",
               TestCodedBurstEpilogueUsesClosingBurstAndAckChirp);
    runner.Add("FlashVoicing.CodedBurstEpilogueContrastsPayloadTailAndStopsHard",
               TestCodedBurstEpilogueContrastsPayloadTailAndStopsHard);
    runner.Add("FlashVoicing.TrimDescriptorTracksNonpayloadSamples", TestTrimDescriptorTracksNonpayloadSamples);
    runner.Add("FlashVoicing.TrimToPayloadPcmExtractsPayloadForDecode", TestTrimToPayloadPcmExtractsPayloadForDecode);
    runner.Add("FlashVoicing.PreambleAndEpilogueSegmentsAreInserted", TestPreambleAndEpilogueSegmentsAreInserted);
    runner.Add("FlashVoicing.TrimRejectsDescriptorMismatch", TestTrimRejectsDescriptorMismatch);
}

}  // namespace flash_voicing_test

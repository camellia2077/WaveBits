#include "test_std_support.h"
#include "test_framework.h"

import bag.flash.phy_clean;
import bag.flash.signal;
import bag.flash.voicing;

#include "flash_voicing_test_support.h"

namespace {

using namespace flash_voicing_test;

void TestNoOpVoicingPreservesPayload() {
    const auto layout = MakePayloadLayout("Hi");
    const auto clean_payload = MakeCleanPayload("Hi");
    const auto voiced = bag::flash::ApplyVoicingToPayload(clean_payload, layout);

    test::AssertEq(voiced.pcm, clean_payload, "No-op voicing should preserve payload PCM.");
    test::AssertEq(
        voiced.descriptor.leading_nonpayload_samples,
        static_cast<std::size_t>(0),
        "No-op voicing should report zero leading non-payload samples.");
    test::AssertEq(
        voiced.descriptor.trailing_nonpayload_samples,
        static_cast<std::size_t>(0),
        "No-op voicing should report zero trailing non-payload samples.");
    test::AssertEq(
        voiced.descriptor.payload_sample_count,
        clean_payload.size(),
        "No-op voicing should preserve payload sample count.");
}

void TestEnvelopeKeepsPayloadLength() {
    const auto layout = MakePayloadLayout("Envelope");
    const auto clean_payload = MakeCleanPayload("Envelope");
    const auto voiced =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeEnvelopeOnlyConfig());

    test::AssertEq(
        voiced.pcm.size(),
        clean_payload.size(),
        "Envelope voicing should preserve total payload length.");
    test::AssertEq(
        voiced.descriptor.payload_sample_count,
        clean_payload.size(),
        "Envelope voicing should preserve descriptor payload length.");
    test::AssertEq(
        voiced.descriptor.leading_nonpayload_samples,
        static_cast<std::size_t>(0),
        "Envelope voicing should keep zero leading non-payload samples.");
    test::AssertEq(
        voiced.descriptor.trailing_nonpayload_samples,
        static_cast<std::size_t>(0),
        "Envelope voicing should keep zero trailing non-payload samples.");
    test::AssertTrue(
        voiced.pcm != clean_payload,
        "Envelope voicing should alter payload shape without changing length.");
}

void TestHarmonicVoicingStaysWithinPcm16Range() {
    const auto layout = MakePayloadLayout("Harmonic");
    const auto clean_payload = MakeCleanPayload("Harmonic");
    const auto voiced =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeHarmonicOnlyConfig());

    test::AssertEq(
        voiced.pcm.size(),
        clean_payload.size(),
        "Harmonic voicing should preserve total payload length.");
    test::AssertTrue(
        voiced.pcm != clean_payload,
        "Harmonic voicing should differ from the clean payload.");
    AssertPcm16Range(voiced.pcm, "Harmonic voicing output");
}

void TestBoundaryClickVoicingIsDeterministic() {
    const auto layout = MakePayloadLayout("AB");
    const auto clean_payload = MakeCleanPayload("AB");
    const auto first =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeClickOnlyConfig());
    const auto second =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeClickOnlyConfig());

    test::AssertEq(
        first.pcm,
        second.pcm,
        "Boundary click voicing should be deterministic for the same input.");
    test::AssertTrue(
        first.pcm != clean_payload,
        "Boundary click voicing should alter payload samples at byte boundaries.");
}

void TestStyledVoicingOutputIsStable() {
    const auto layout = MakePayloadLayout("FlipBits");
    const auto clean_payload = MakeCleanPayload("FlipBits");
    const auto first =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeStyledConfig());
    const auto second =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeStyledConfig());

    test::AssertEq(
        first.pcm,
        second.pcm,
        "Styled voicing output should remain identical across repeated runs.");
    test::AssertEq(
        first.descriptor.payload_sample_count,
        clean_payload.size(),
        "Styled voicing should preserve descriptor payload length.");
    AssertPcm16Range(first.pcm, "Styled voicing output");
}

void TestDefaultVoicingMatchesExplicitCodedBurst() {
    const auto layout = MakePayloadLayout("FlipBits");
    const auto clean_payload = MakeCleanPayload("FlipBits");
    const auto default_voiced =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeStyledConfig());
    const auto explicit_coded_burst =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kCodedBurst,
            MakeStyledConfig());

    test::AssertEq(
        default_voiced.pcm,
        explicit_coded_burst.pcm,
        "Default flash voicing should remain identical to explicit coded_burst style.");
    test::AssertEq(
        default_voiced.descriptor.payload_sample_count,
        explicit_coded_burst.descriptor.payload_sample_count,
        "Default flash voicing should preserve the coded_burst descriptor.");
}

void TestFlavorVoicingMatchesExplicitRitualConfiguration() {
    const auto config = MakeStyledShellConfig();
    const auto layout = MakePayloadLayout("Flavor");
    const auto clean_payload = MakeCleanPayload("Flavor");
    const auto flavored =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kRitualChant,
            config);
    const auto repeated =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kRitualChant,
            config);

    test::AssertEq(
        flavored.pcm,
        repeated.pcm,
        "Flavor-based voicing should remain deterministic for ritual_chant.");
    test::AssertEq(
        flavored.descriptor.payload_sample_count,
        repeated.descriptor.payload_sample_count,
        "Flavor-based voicing should preserve the same descriptor payload size across repeated calls.");
}

void TestRitualChantDiffersButDecodesLikeCodedBurst() {
    const auto config = MakeStyledShellConfig();
    const auto layout = MakePayloadLayout("Command");
    const auto clean_payload = MakeCleanPayload("Command");
    const auto coded_burst =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kCodedBurst,
            config);
    const auto ritual_chant =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kRitualChant,
            config);

    test::AssertTrue(
        coded_burst.pcm != ritual_chant.pcm,
        "Ritual chant should sound different from coded_burst for the same payload.");

    const auto coded_trimmed = bag::flash::TrimToPayloadPcm(coded_burst.pcm, coded_burst.descriptor);
    const auto ritual_trimmed = bag::flash::TrimToPayloadPcm(ritual_chant.pcm, ritual_chant.descriptor);

    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(coded_trimmed, MakeSignalConfig()),
        AsBytes("Command"),
        "Coded burst should still decode to the original bytes.");
    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(ritual_trimmed, MakeSignalConfig()),
        AsBytes("Command"),
        "Ritual chant should decode to the same bytes as coded_burst.");
}

void TestByteBoundaryAccentRemainsStrongerThanNibbleAccent() {
    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeClickOnlyConfig());

    const auto window_for_chunk = [&](std::size_t chunk_index) {
        const auto& chunk = layout.chunks[chunk_index];
        const std::size_t window =
            std::clamp(chunk.sample_count / static_cast<std::size_t>(96),
                       static_cast<std::size_t>(6),
                       static_cast<std::size_t>(24));
        return AverageAbsoluteDelta(
            voiced.pcm,
            clean_payload,
            chunk.sample_offset,
            chunk.sample_offset + window);
    };

    const double nibble_accent = window_for_chunk(static_cast<std::size_t>(4));
    const double byte_accent = window_for_chunk(static_cast<std::size_t>(8));

    test::AssertTrue(
        nibble_accent > 0.0,
        "Nibble grouping should introduce a detectable accent at four-bit boundaries.");
    test::AssertTrue(
        byte_accent > nibble_accent,
        "Byte boundary accent should remain stronger than nibble boundary accent.");
}

}  // namespace

namespace flash_voicing_test {

void RegisterFlashVoicingPayloadTests(test::Runner& runner) {
    runner.Add("FlashVoicing.NoOpVoicingPreservesPayload", TestNoOpVoicingPreservesPayload);
    runner.Add("FlashVoicing.EnvelopeKeepsPayloadLength", TestEnvelopeKeepsPayloadLength);
    runner.Add("FlashVoicing.HarmonicVoicingStaysWithinPcm16Range", TestHarmonicVoicingStaysWithinPcm16Range);
    runner.Add("FlashVoicing.BoundaryClickVoicingIsDeterministic", TestBoundaryClickVoicingIsDeterministic);
    runner.Add("FlashVoicing.StyledVoicingOutputIsStable", TestStyledVoicingOutputIsStable);
    runner.Add("FlashVoicing.DefaultVoicingMatchesExplicitCodedBurst", TestDefaultVoicingMatchesExplicitCodedBurst);
    runner.Add("FlashVoicing.FlavorVoicingMatchesExplicitRitualConfiguration",
               TestFlavorVoicingMatchesExplicitRitualConfiguration);
    runner.Add("FlashVoicing.RitualChantDiffersButDecodesLikeCodedBurst",
               TestRitualChantDiffersButDecodesLikeCodedBurst);
    runner.Add("FlashVoicing.ByteBoundaryAccentRemainsStrongerThanNibbleAccent",
               TestByteBoundaryAccentRemainsStrongerThanNibbleAccent);
}

}  // namespace flash_voicing_test

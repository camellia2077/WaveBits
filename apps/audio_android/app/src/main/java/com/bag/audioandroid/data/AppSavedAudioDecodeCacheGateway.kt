package com.bag.audioandroid.data

import android.content.Context
import com.bag.audioandroid.domain.DecodedPayloadViewData
import com.bag.audioandroid.domain.FlashSignalInfo
import com.bag.audioandroid.domain.GeneratedAudioMetadata
import com.bag.audioandroid.domain.PayloadFollowBinaryGroupTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowByteTimelineEntry
import com.bag.audioandroid.domain.PayloadFollowViewData
import com.bag.audioandroid.domain.SavedAudioDecodeCacheGateway
import com.bag.audioandroid.domain.SavedAudioDecodedCacheEntry
import com.bag.audioandroid.domain.SavedAudioItem
import com.bag.audioandroid.domain.TextFollowLineRawSegmentViewData
import com.bag.audioandroid.domain.TextFollowLineTokenRangeViewData
import com.bag.audioandroid.domain.TextFollowLyricLineTimelineEntry
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.domain.TextFollowRawSegmentViewData
import com.bag.audioandroid.domain.TextFollowTimelineEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AppSavedAudioDecodeCacheGateway(
    context: Context,
) : SavedAudioDecodeCacheGateway {
    private val cacheDirectory = File(context.filesDir, CACHE_DIRECTORY_NAME).apply { mkdirs() }

    override fun read(
        item: SavedAudioItem,
        metadata: GeneratedAudioMetadata?,
    ): SavedAudioDecodedCacheEntry? {
        val file = cacheFileForItemId(item.itemId)
        if (!file.exists()) {
            return null
        }
        return runCatching {
            val json = JSONObject(file.readText())
            val fingerprint = json.optString(JSON_KEY_FINGERPRINT)
            if (fingerprint != buildFingerprint(item, metadata)) {
                file.delete()
                return null
            }
            SavedAudioDecodedCacheEntry(
                decodedPayload = json.optJSONObject(JSON_KEY_DECODED_PAYLOAD)?.toDecodedPayload() ?: DecodedPayloadViewData.Empty,
                followData = json.optJSONObject(JSON_KEY_FOLLOW_DATA)?.toFollowData() ?: PayloadFollowViewData.Empty,
                flashSignalInfo = json.optJSONObject(JSON_KEY_FLASH_SIGNAL_INFO)?.toFlashSignalInfo() ?: FlashSignalInfo.Empty,
            )
        }.getOrNull()
    }

    override fun write(
        item: SavedAudioItem,
        metadata: GeneratedAudioMetadata?,
        decodedPayload: DecodedPayloadViewData,
        followData: PayloadFollowViewData,
        flashSignalInfo: FlashSignalInfo,
    ) {
        val file = cacheFileForItemId(item.itemId)
        val json =
            JSONObject()
                .put(JSON_KEY_FINGERPRINT, buildFingerprint(item, metadata))
                .put(JSON_KEY_DECODED_PAYLOAD, decodedPayload.toJson())
                .put(JSON_KEY_FOLLOW_DATA, followData.toJson())
                .put(JSON_KEY_FLASH_SIGNAL_INFO, flashSignalInfo.toJson())
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.toString())
        }
    }

    override fun delete(itemId: String) {
        runCatching { cacheFileForItemId(itemId).delete() }
    }

    override fun prune(validItemIds: Set<String>) {
        val files = cacheDirectory.listFiles() ?: return
        files.forEach { file ->
            val itemId = file.name.removeSuffix(CACHE_FILE_SUFFIX)
            if (itemId !in validItemIds) {
                runCatching { file.delete() }
            }
        }
    }

    private fun cacheFileForItemId(itemId: String): File = File(cacheDirectory, "$itemId$CACHE_FILE_SUFFIX")

    private fun buildFingerprint(
        item: SavedAudioItem,
        metadata: GeneratedAudioMetadata?,
    ): String =
        listOf(
            item.itemId,
            item.uriString,
            item.modeWireName,
            item.durationMs.toString(),
            item.savedAtEpochSeconds.toString(),
            (item.fileSizeBytes ?: -1L).toString(),
            (item.sampleRateHz ?: -1).toString(),
            (item.payloadByteCount ?: -1).toString(),
            item.flashVoicingStyle?.id.orEmpty(),
            (metadata?.version ?: -1).toString(),
            metadata?.createdAtIsoUtc.orEmpty(),
            (metadata?.sampleRateHz ?: -1).toString(),
            (metadata?.frameSamples ?: -1).toString(),
            (metadata?.pcmSampleCount ?: -1).toString(),
            (metadata?.payloadByteCount ?: -1).toString(),
            metadata?.flashVoicingStyle?.id.orEmpty(),
        ).joinToString(separator = "|")

    private companion object {
        const val CACHE_DIRECTORY_NAME = "saved-audio-decode-cache"
        const val CACHE_FILE_SUFFIX = ".json"
        const val JSON_KEY_FINGERPRINT = "fingerprint"
        const val JSON_KEY_DECODED_PAYLOAD = "decoded_payload"
        const val JSON_KEY_FOLLOW_DATA = "follow_data"
        const val JSON_KEY_FLASH_SIGNAL_INFO = "flash_signal_info"
    }
}

private fun DecodedPayloadViewData.toJson() =
    JSONObject()
        .put("text", text)
        .put("raw_bytes_hex", rawBytesHex)
        .put("raw_bits_binary", rawBitsBinary)
        .put("text_decode_status_code", textDecodeStatusCode)
        .put("raw_payload_available", rawPayloadAvailable)

private fun JSONObject.toDecodedPayload() =
    DecodedPayloadViewData(
        text = optString("text"),
        rawBytesHex = optString("raw_bytes_hex"),
        rawBitsBinary = optString("raw_bits_binary"),
        textDecodeStatusCode = optInt("text_decode_status_code"),
        rawPayloadAvailable = optBoolean("raw_payload_available"),
    )

private fun FlashSignalInfo.toJson() =
    JSONObject()
        .put("low_carrier_hz", lowCarrierHz)
        .put("high_carrier_hz", highCarrierHz)
        .put("bit_duration_samples", bitDurationSamples)
        .put("payload_silence", payloadSilence)
        .put("decode_path", decodePath)
        .put("available", available)

private fun JSONObject.toFlashSignalInfo() =
    FlashSignalInfo(
        lowCarrierHz = optString("low_carrier_hz"),
        highCarrierHz = optString("high_carrier_hz"),
        bitDurationSamples = optString("bit_duration_samples"),
        payloadSilence = optString("payload_silence"),
        decodePath = optString("decode_path"),
        available = optBoolean("available"),
    )

private fun PayloadFollowViewData.toJson() =
    JSONObject()
        .put("text_tokens", JSONArray(textTokens))
        .put("text_token_timeline", JSONArray().apply { textTokenTimeline.forEach { put(it.toJson()) } })
        .put("text_raw_segments", JSONArray().apply { textRawSegments.forEach { put(it.toJson()) } })
        .put("text_raw_display_units", JSONArray().apply { textRawDisplayUnits.forEach { put(it.toJson()) } })
        .put("text_follow_available", textFollowAvailable)
        .put("lyric_lines", JSONArray(lyricLines))
        .put("lyric_line_timeline", JSONArray().apply { lyricLineTimeline.forEach { put(it.toJson()) } })
        .put("line_token_ranges", JSONArray().apply { lineTokenRanges.forEach { put(it.toJson()) } })
        .put("line_raw_segments", JSONArray().apply { lineRawSegments.forEach { put(it.toJson()) } })
        .put("lyric_line_follow_available", lyricLineFollowAvailable)
        .put("hex_tokens", JSONArray(hexTokens))
        .put("binary_tokens", JSONArray(binaryTokens))
        .put("byte_timeline", JSONArray().apply { byteTimeline.forEach { put(it.toJson()) } })
        .put("binary_group_timeline", JSONArray().apply { binaryGroupTimeline.forEach { put(it.toJson()) } })
        .put("payload_begin_sample", payloadBeginSample)
        .put("payload_sample_count", payloadSampleCount)
        .put("total_pcm_sample_count", totalPcmSampleCount)
        .put("follow_available", followAvailable)

private fun JSONObject.toFollowData() =
    PayloadFollowViewData(
        textTokens = optJSONArray("text_tokens").toStringList(),
        textTokenTimeline = optJSONArray("text_token_timeline").toObjectList { it.toTextFollowTimelineEntry() },
        textRawSegments = optJSONArray("text_raw_segments").toObjectList { it.toTextFollowRawSegmentViewData() },
        textRawDisplayUnits = optJSONArray("text_raw_display_units").toObjectList { it.toTextFollowRawDisplayUnitViewData() },
        textFollowAvailable = optBoolean("text_follow_available"),
        lyricLines = optJSONArray("lyric_lines").toStringList(),
        lyricLineTimeline = optJSONArray("lyric_line_timeline").toObjectList { it.toTextFollowLyricLineTimelineEntry() },
        lineTokenRanges = optJSONArray("line_token_ranges").toObjectList { it.toTextFollowLineTokenRangeViewData() },
        lineRawSegments = optJSONArray("line_raw_segments").toObjectList { it.toTextFollowLineRawSegmentViewData() },
        lyricLineFollowAvailable = optBoolean("lyric_line_follow_available"),
        hexTokens = optJSONArray("hex_tokens").toStringList(),
        binaryTokens = optJSONArray("binary_tokens").toStringList(),
        byteTimeline = optJSONArray("byte_timeline").toObjectList { it.toPayloadFollowByteTimelineEntry() },
        binaryGroupTimeline = optJSONArray("binary_group_timeline").toObjectList { it.toPayloadFollowBinaryGroupTimelineEntry() },
        payloadBeginSample = optInt("payload_begin_sample"),
        payloadSampleCount = optInt("payload_sample_count"),
        totalPcmSampleCount = optInt("total_pcm_sample_count"),
        followAvailable = optBoolean("follow_available"),
    )

private fun TextFollowTimelineEntry.toJson() =
    JSONObject()
        .put("start_sample", startSample)
        .put("sample_count", sampleCount)
        .put("token_index", tokenIndex)

private fun JSONObject.toTextFollowTimelineEntry() =
    TextFollowTimelineEntry(
        startSample = optInt("start_sample"),
        sampleCount = optInt("sample_count"),
        tokenIndex = optInt("token_index"),
    )

private fun TextFollowRawSegmentViewData.toJson() =
    JSONObject()
        .put("token_index", tokenIndex)
        .put("start_sample", startSample)
        .put("sample_count", sampleCount)
        .put("byte_offset", byteOffset)
        .put("byte_count", byteCount)
        .put("hex_text", hexText)
        .put("binary_text", binaryText)

private fun JSONObject.toTextFollowRawSegmentViewData() =
    TextFollowRawSegmentViewData(
        tokenIndex = optInt("token_index"),
        startSample = optInt("start_sample"),
        sampleCount = optInt("sample_count"),
        byteOffset = optInt("byte_offset"),
        byteCount = optInt("byte_count"),
        hexText = optString("hex_text"),
        binaryText = optString("binary_text"),
    )

private fun TextFollowRawDisplayUnitViewData.toJson() =
    JSONObject()
        .put("token_index", tokenIndex)
        .put("start_sample", startSample)
        .put("sample_count", sampleCount)
        .put("byte_index_within_token", byteIndexWithinToken)
        .put("byte_offset", byteOffset)
        .put("byte_count", byteCount)
        .put("hex_text", hexText)
        .put("binary_text", binaryText)

private fun JSONObject.toTextFollowRawDisplayUnitViewData() =
    TextFollowRawDisplayUnitViewData(
        tokenIndex = optInt("token_index"),
        startSample = optInt("start_sample"),
        sampleCount = optInt("sample_count"),
        byteIndexWithinToken = optInt("byte_index_within_token"),
        byteOffset = optInt("byte_offset"),
        byteCount = optInt("byte_count"),
        hexText = optString("hex_text"),
        binaryText = optString("binary_text"),
    )

private fun TextFollowLyricLineTimelineEntry.toJson() =
    JSONObject()
        .put("start_sample", startSample)
        .put("sample_count", sampleCount)
        .put("line_index", lineIndex)

private fun JSONObject.toTextFollowLyricLineTimelineEntry() =
    TextFollowLyricLineTimelineEntry(
        startSample = optInt("start_sample"),
        sampleCount = optInt("sample_count"),
        lineIndex = optInt("line_index"),
    )

private fun TextFollowLineTokenRangeViewData.toJson() =
    JSONObject()
        .put("line_index", lineIndex)
        .put("token_begin_index", tokenBeginIndex)
        .put("token_count", tokenCount)

private fun JSONObject.toTextFollowLineTokenRangeViewData() =
    TextFollowLineTokenRangeViewData(
        lineIndex = optInt("line_index"),
        tokenBeginIndex = optInt("token_begin_index"),
        tokenCount = optInt("token_count"),
    )

private fun TextFollowLineRawSegmentViewData.toJson() =
    JSONObject()
        .put("line_index", lineIndex)
        .put("start_sample", startSample)
        .put("sample_count", sampleCount)
        .put("byte_offset", byteOffset)
        .put("byte_count", byteCount)
        .put("hex_text", hexText)
        .put("binary_text", binaryText)

private fun JSONObject.toTextFollowLineRawSegmentViewData() =
    TextFollowLineRawSegmentViewData(
        lineIndex = optInt("line_index"),
        startSample = optInt("start_sample"),
        sampleCount = optInt("sample_count"),
        byteOffset = optInt("byte_offset"),
        byteCount = optInt("byte_count"),
        hexText = optString("hex_text"),
        binaryText = optString("binary_text"),
    )

private fun PayloadFollowByteTimelineEntry.toJson() =
    JSONObject()
        .put("start_sample", startSample)
        .put("sample_count", sampleCount)
        .put("byte_index", byteIndex)

private fun JSONObject.toPayloadFollowByteTimelineEntry() =
    PayloadFollowByteTimelineEntry(
        startSample = optInt("start_sample"),
        sampleCount = optInt("sample_count"),
        byteIndex = optInt("byte_index"),
    )

private fun PayloadFollowBinaryGroupTimelineEntry.toJson() =
    JSONObject()
        .put("start_sample", startSample)
        .put("sample_count", sampleCount)
        .put("group_index", groupIndex)
        .put("bit_offset", bitOffset)
        .put("bit_count", bitCount)

private fun JSONObject.toPayloadFollowBinaryGroupTimelineEntry() =
    PayloadFollowBinaryGroupTimelineEntry(
        startSample = optInt("start_sample"),
        sampleCount = optInt("sample_count"),
        groupIndex = optInt("group_index"),
        bitOffset = optInt("bit_offset"),
        bitCount = optInt("bit_count"),
    )

private fun JSONArray?.toStringList(): List<String> =
    if (this == null) {
        emptyList()
    } else {
        buildList(length()) {
            for (index in 0 until length()) {
                add(optString(index))
            }
        }
    }

private inline fun <T> JSONArray?.toObjectList(transform: (JSONObject) -> T): List<T> =
    if (this == null) {
        emptyList()
    } else {
        buildList(length()) {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(transform(item))
            }
        }
    }

package com.bag.audioandroid.ui

import com.bag.audioandroid.data.buildWaveformPreviewFromPcmFile
import com.bag.audioandroid.domain.GeneratedAudioCacheGateway
import com.bag.audioandroid.domain.GeneratedAudioPcmCacheWriter

internal class SegmentedEncodeAudioAssembler private constructor(
    private val writer: GeneratedAudioPcmCacheWriter,
    private val previewPointCount: Int,
) {
    private val mutableSegmentSampleCounts = ArrayList<Int>()
    var totalPcmSamples: Int = 0
        private set

    val filePath: String
        get() = writer.filePath

    val segmentSampleCounts: List<Int>
        get() = mutableSegmentSampleCounts

    fun appendSegment(pcm: ShortArray) {
        writer.appendPcm(pcm)
        totalPcmSamples += pcm.size
        mutableSegmentSampleCounts += pcm.size
    }

    fun abort() {
        writer.abort()
    }

    fun finish(): SegmentedEncodedAudioFile {
        writer.finish()
        return SegmentedEncodedAudioFile(
            filePath = writer.filePath,
            totalSamples = totalPcmSamples,
            segmentSampleCounts = segmentSampleCounts,
            waveformPcm =
                buildWaveformPreviewFromPcmFile(
                    pcmFilePath = writer.filePath,
                    totalSamples = totalPcmSamples,
                    targetPoints = previewPointCount,
                ),
        )
    }

    companion object {
        fun create(
            generatedAudioCacheGateway: GeneratedAudioCacheGateway,
            modeWireName: String,
            previewPointCount: Int,
        ): SegmentedEncodeAudioAssembler =
            SegmentedEncodeAudioAssembler(
                writer = generatedAudioCacheGateway.createPcmCacheWriter(modeWireName),
                previewPointCount = previewPointCount,
            )
    }
}

internal data class SegmentedEncodedAudioFile(
    val filePath: String,
    val totalSamples: Int,
    val segmentSampleCounts: List<Int>,
    val waveformPcm: ShortArray,
)

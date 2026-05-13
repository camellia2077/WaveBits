package com.bag.audioandroid.domain

interface GeneratedAudioCacheGateway {
    /**
     * Product policy: generated-audio cache must stay effectively empty.
     *
     * This app is explicitly optimized for tiny install/storage footprint.
     * Generated PCM cache is allowed only as a short-lived implementation detail
     * for the single result that is actively in use right now. Do not relax this
     * policy without explicit product approval.
     */
    fun createPcmCacheWriter(modeWireName: String): GeneratedAudioPcmCacheWriter

    fun deleteCachedFile(path: String?)

    fun pruneCachedFiles(retainedPaths: Set<String> = emptySet())
}

interface GeneratedAudioPcmCacheWriter {
    val filePath: String

    fun appendPcm(pcm: ShortArray)

    fun finish()

    fun abort()
}

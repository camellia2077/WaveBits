package com.bag.audioandroid.domain

interface GeneratedAudioCacheGateway {
    fun createPcmCacheWriter(modeWireName: String): GeneratedAudioPcmCacheWriter

    fun deleteCachedFile(path: String?)
}

interface GeneratedAudioPcmCacheWriter {
    val filePath: String

    fun appendPcm(pcm: ShortArray)

    fun finish()

    fun abort()
}

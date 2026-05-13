package com.bag.audioandroid.domain

/**
 * Design intent:
 * - Saved media files remain the source of truth.
 * - Decoded text/lyrics may be cached only in app-private storage.
 * - This avoids rewriting the Saved media file while preventing repeated
 *   re-decode work every time the user reopens the same clip.
 */
interface SavedAudioDecodeCacheGateway {
    fun read(
        item: SavedAudioItem,
        metadata: GeneratedAudioMetadata?,
    ): SavedAudioDecodedCacheEntry?

    fun write(
        item: SavedAudioItem,
        metadata: GeneratedAudioMetadata?,
        decodedPayload: DecodedPayloadViewData,
        followData: PayloadFollowViewData,
        flashSignalInfo: FlashSignalInfo,
    )

    fun delete(itemId: String)

    fun prune(validItemIds: Set<String>)
}

data class SavedAudioDecodedCacheEntry(
    val decodedPayload: DecodedPayloadViewData,
    val followData: PayloadFollowViewData,
    val flashSignalInfo: FlashSignalInfo,
)

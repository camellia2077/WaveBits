package com.bag.audioandroid.ui.model

import androidx.annotation.StringRes
import com.bag.audioandroid.R

enum class PlaybackSequenceMode(
    val id: String,
    @param:StringRes val labelResId: Int
) {
    Normal(
        id = "normal",
        labelResId = R.string.audio_action_normal_playback
    ),
    RepeatOne(
        id = "repeat_one",
        labelResId = R.string.audio_action_repeat_one
    ),
    RepeatList(
        id = "repeat_list",
        labelResId = R.string.audio_action_repeat_list
    ),
    Shuffle(
        id = "shuffle",
        labelResId = R.string.audio_action_shuffle_playback
    );

    fun next(): PlaybackSequenceMode =
        when (this) {
            Normal -> RepeatOne
            RepeatOne -> RepeatList
            RepeatList -> Shuffle
            Shuffle -> Normal
        }

    companion object {
        fun fromId(id: String?): PlaybackSequenceMode =
            entries.firstOrNull { it.id == id } ?: Normal
    }
}

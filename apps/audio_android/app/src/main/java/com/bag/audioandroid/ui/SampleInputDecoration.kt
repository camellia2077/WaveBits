package com.bag.audioandroid.ui

import com.bag.audioandroid.ui.model.SampleFlavor
import com.bag.audioandroid.ui.model.TransportModeOption
import com.bag.audioandroid.ui.state.ModeAudioSessionState
import com.bag.audioandroid.ui.state.SampleEmojiShuffleState
import kotlin.random.Random

internal data class SampleEmojiPrefix(
    val emoji: String,
    val state: SampleEmojiShuffleState,
)

internal fun nextSampleEmojiPrefix(
    mode: TransportModeOption,
    flavor: SampleFlavor,
    isDecorationEnabled: Boolean,
    currentState: SampleEmojiShuffleState?,
    random: Random = Random.Default,
): SampleEmojiPrefix? {
    if (!isDecorationEnabled) {
        return null
    }
    val style = sampleEmojiStyleFor(flavor) ?: return null
    if (mode != TransportModeOption.Flash || style.rotatingEmojis.isEmpty()) {
        return null
    }
    val activeState =
        currentState
            ?.takeIf { it.shuffledEmojis.toSet() == style.rotatingEmojis.toSet() }
            ?: SampleEmojiShuffleState(
                shuffledEmojis = style.rotatingEmojis.shuffled(random),
                nextEmojiIndex = 0,
                lastPresentedEmoji = null,
            )
    val reshuffledWhenConsumed =
        if (activeState.nextEmojiIndex >= activeState.shuffledEmojis.size) {
            val nextDeck = activeState.shuffledEmojis.shuffled(random).toMutableList()
            val avoid = activeState.lastPresentedEmoji
            if (avoid != null && nextDeck.size > 1 && nextDeck.first() == avoid) {
                val swapIndex = nextDeck.indexOfFirst { it != avoid }
                if (swapIndex > 0) {
                    val first = nextDeck.first()
                    nextDeck[0] = nextDeck[swapIndex]
                    nextDeck[swapIndex] = first
                }
            }
            SampleEmojiShuffleState(
                shuffledEmojis = nextDeck,
                nextEmojiIndex = 0,
                lastPresentedEmoji = activeState.lastPresentedEmoji,
            )
        } else {
            activeState
        }
    val emoji = reshuffledWhenConsumed.shuffledEmojis[reshuffledWhenConsumed.nextEmojiIndex]
    return SampleEmojiPrefix(
        emoji = emoji,
        state =
            reshuffledWhenConsumed.copy(
                nextEmojiIndex = reshuffledWhenConsumed.nextEmojiIndex + 1,
                lastPresentedEmoji = emoji,
            ),
    )
}

internal fun withSampleEmojiPrefix(
    text: String,
    emoji: String?,
): String {
    if (emoji.isNullOrBlank()) {
        return text
    }
    val stripped = text.trimStart().replace(Regex("^[\\p{So}\\p{Sk}\\uFE0F]+\\s*"), "")
    return "$emoji $stripped"
}

internal fun removeAppliedSampleEmojiPrefix(session: ModeAudioSessionState): ModeAudioSessionState {
    val prefix = session.appliedSampleEmojiPrefix ?: return session
    val textWithoutPrefix =
        if (session.inputText.startsWith("$prefix ")) {
            session.inputText.removePrefix("$prefix ").trimStart()
        } else {
            session.inputText
        }
    return session.copy(
        inputText = textWithoutPrefix,
        appliedSampleEmojiPrefix = null,
    )
}

internal fun applySampleEmojiDecoration(
    session: ModeAudioSessionState,
    mode: TransportModeOption,
    flavor: SampleFlavor,
    isDecorationEnabled: Boolean,
    random: Random = Random.Default,
): ModeAudioSessionState {
    if (session.sampleInputId == null) {
        return session
    }
    val withoutAppliedPrefix = removeAppliedSampleEmojiPrefix(session)
    val emojiPrefix =
        nextSampleEmojiPrefix(
            mode = mode,
            flavor = flavor,
            isDecorationEnabled = isDecorationEnabled,
            currentState = withoutAppliedPrefix.sampleEmojiShuffleState,
            random = random,
        )
    if (emojiPrefix == null) {
        return withoutAppliedPrefix
    }
    return withoutAppliedPrefix.copy(
        inputText = withSampleEmojiPrefix(withoutAppliedPrefix.inputText, emojiPrefix.emoji),
        sampleEmojiShuffleState = emojiPrefix.state,
        appliedSampleEmojiPrefix = emojiPrefix.emoji,
    )
}

private fun sampleEmojiStyleFor(flavor: SampleFlavor): FlavorEmojiStyle? =
    when (flavor) {
        SampleFlavor.SacredMachine -> SacredMachineEmojiStyle
        SampleFlavor.AncientDynasty -> AncientDynastyEmojiStyle
        SampleFlavor.LabyrinthOfMutability -> LabyrinthEmojiStyle
        SampleFlavor.ExquisiteFall -> ExquisiteEmojiStyle
        SampleFlavor.ImmortalRot -> ImmortalEmojiStyle
        SampleFlavor.ScarletCarnage -> ScarletEmojiStyle
    }

private data class FlavorEmojiStyle(
    val rotatingEmojis: List<String>,
)

private val SacredMachineEmojiStyle =
    FlavorEmojiStyle(
        rotatingEmojis =
            listOf(
                "\uD83D\uDEE0\uFE0F", // tool
                "\uD83D\uDD29", // nut and bolt
                "\u26D3\uFE0F", // chains
                "\uD83E\uDDEA", // test tube
                "\uD83D\uDD6F\uFE0F", // candle
                "\uD83D\uDCBE", // floppy disk
                "\uD83D\uDEF0\uFE0F", // satellite
            ),
    )

private val AncientDynastyEmojiStyle =
    FlavorEmojiStyle(
        rotatingEmojis =
            listOf(
                "\u26B1\uFE0F", // funeral urn
                "\uD83D\uDD3A", // red triangle pointed up
                "\uD83E\uDDFF", // nazar amulet
                "\uD83D\uDCA0", // diamond with a dot
                "\u2600\uFE0F", // sun
            ),
    )

private val LabyrinthEmojiStyle =
    FlavorEmojiStyle(
        rotatingEmojis =
            listOf(
                "\uD83C\uDF00", // cyclone
                "\uD83D\uDC41\uFE0F", // eye
                "\uD83C\uDFAD", // performing arts
                "\uD83E\uDDE9", // puzzle piece
                "\uD83D\uDD2E", // crystal ball
            ),
    )

private val ExquisiteEmojiStyle =
    FlavorEmojiStyle(
        rotatingEmojis =
            listOf(
                "\uD83D\uDC8E", // gem stone
                "\uD83C\uDF39", // rose
                "\uD83C\uDFBC", // musical score
                "\uD83E\uDE9E", // mirror
                "\u2728", // sparkles
            ),
    )

private val ImmortalEmojiStyle =
    FlavorEmojiStyle(
        rotatingEmojis =
            listOf(
                "\uD83E\uDEB0", // fly
                "\u2623\uFE0F", // biohazard
                "\uD83E\uDDA0", // microbe
                "\uD83D\uDC80", // skull
            ),
    )

private val ScarletEmojiStyle =
    FlavorEmojiStyle(
        rotatingEmojis =
            listOf(
                "\uD83E\uDE78", // drop of blood
                "\uD83D\uDDE1\uFE0F", // dagger
                "\uD83D\uDEE1\uFE0F", // shield
                "\uD83D\uDD25", // fire
            ),
    )

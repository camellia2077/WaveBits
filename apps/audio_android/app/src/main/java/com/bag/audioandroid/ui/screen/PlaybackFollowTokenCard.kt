package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.domain.TextFollowRawDisplayUnitViewData
import com.bag.audioandroid.ui.playbackLyricsAccentTextColor
import com.bag.audioandroid.ui.theme.appThemeVisualTokens

@Composable
internal fun PlaybackFollowTokenCard(
    token: String,
    rawDisplayUnits: List<TextFollowRawDisplayUnitViewData>,
    annotationMode: PlaybackFollowViewMode,
    isActive: Boolean,
    activeByteIndexWithinToken: Int,
    activeBitIndexWithinByte: Int = -1,
    isActiveBitTone: Boolean = false,
    isPast: Boolean = false,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    modifier: Modifier = Modifier,
) {
    val visualTokens = appThemeVisualTokens()
    val activeAnnotationContainer = Color.Transparent
    val lyricsAccentTextColor = playbackLyricsAccentTextColor()
    val focusColor = MaterialTheme.colorScheme.primary
    val onFocusColor = MaterialTheme.colorScheme.onPrimary
    val activeAnnotationTint = lyricsAccentTextColor

    val containerColor =
        when {
            isActive -> visualTokens.followTokenContainerColor
            else -> Color.Transparent
        }

    val tokenColor =
        when {
            isActive -> lyricsAccentTextColor
            isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.onSurface
        }
    val cardModifier =
        if (onClick != null) {
            modifier.clickable(onClick = onClick)
        } else {
            modifier
        }

    val inactiveRawColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isPast) 0.5f else 1.0f)
    val inactiveCharacterColor = tokenColor
    val annotationByteGroups = annotationByteGroupsForMode(annotationMode, rawDisplayUnits)
    val characterDisplayUnits = remember(token) { characterDisplayUnits(token) }
    val tokenDisplayText =
        remember(
            token,
            characterDisplayUnits,
            activeByteIndexWithinToken,
            isActive,
            inactiveCharacterColor,
            lyricsAccentTextColor,
        ) {
            buildAnnotatedString {
                if (characterDisplayUnits.isEmpty()) {
                    append(token)
                    return@buildAnnotatedString
                }
                characterDisplayUnits.forEach { unit ->
                    val isActiveCharacter =
                        isActive &&
                            activeByteIndexWithinToken >= unit.byteStartIndexWithinToken &&
                            activeByteIndexWithinToken <
                            unit.byteStartIndexWithinToken + unit.byteCount
                    withStyle(
                        SpanStyle(
                            color = if (isActiveCharacter) onFocusColor else inactiveCharacterColor,
                            background = if (isActiveCharacter) focusColor else Color.Transparent,
                            fontWeight = if (isActiveCharacter) FontWeight.ExtraBold else FontWeight.Bold,
                        ),
                    ) {
                        if (isActiveCharacter) {
                            append(unit.text)
                        } else {
                            append(unit.text)
                        }
                    }
                }
            }
        }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.large,
        modifier =
            cardModifier
                .widthIn(min = PlaybackFollowTokenCardMinimumWidth, max = PlaybackFollowTokenCardMaximumWidth)
                .testTag(
                    testTag ?: if (isActive) {
                        "follow-token-active"
                    } else {
                        "follow-token"
                    },
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = tokenDisplayText,
                color = tokenColor,
                style =
                    if (isActive) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Surface(
                color =
                    if (isActive) {
                        activeAnnotationContainer
                    } else {
                        Color.Transparent
                    },
                contentColor =
                    if (isActive) {
                        activeAnnotationTint
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                PlaybackFollowAnnotationRows(
                    token = token,
                    rawDisplayUnits = rawDisplayUnits,
                    annotationMode = annotationMode,
                    annotationByteGroups = annotationByteGroups,
                    characterDisplayUnits = characterDisplayUnits,
                    isActive = isActive,
                    activeByteIndexWithinToken = activeByteIndexWithinToken,
                    activeBitIndexWithinByte = activeBitIndexWithinByte,
                    isActiveBitTone = isActiveBitTone,
                    inactiveRawColor = inactiveRawColor,
                    focusColor = focusColor,
                    onFocusColor = onFocusColor,
                )
            }
        }
    }
}

private val PlaybackFollowTokenCardMinimumWidth = 92.dp
private val PlaybackFollowTokenCardMaximumWidth = 360.dp

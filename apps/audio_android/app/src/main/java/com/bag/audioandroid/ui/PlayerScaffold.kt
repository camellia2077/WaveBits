package com.bag.audioandroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.pow

private object PlayerScaffoldDefaults {
    val dockHorizontalPadding = 12.dp
    val bottomNavigationBarHeight = 80.dp
    val miniPlayerHeight = 72.dp
    val dockSectionSpacing = 8.dp
    val contentBottomBreath = 16.dp
    val snackbarBottomSpacing = 12.dp
}

/**
 * The scaffold uses a bottom-aligned dock layered over the content viewport so the mini-player
 * can stay visible after encode completes. Content gets matching bottom padding and can scroll
 * behind the dock without losing access to its last items.
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun PlayerScaffold(
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isDemoModeEnabled: Boolean = false,
    demoTouchFillColor: Color = MaterialTheme.colorScheme.onSurface,
    demoTouchStrokeColor: Color = MaterialTheme.colorScheme.primary,
    snackbarHost: (@Composable () -> Unit)? = null,
    miniPlayer: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val safeTopPadding = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val bottomNavigationHeight = PlayerScaffoldDefaults.bottomNavigationBarHeight
    val miniPlayerHeight = if (miniPlayer != null) PlayerScaffoldDefaults.miniPlayerHeight else 0.dp
    val dockSpacing = if (miniPlayer != null) PlayerScaffoldDefaults.dockSectionSpacing else 0.dp
    val contentBottomPadding =
        bottomNavigationHeight + miniPlayerHeight + dockSpacing + PlayerScaffoldDefaults.contentBottomBreath
    val snackbarBottomPadding =
        bottomNavigationHeight + miniPlayerHeight + dockSpacing + PlayerScaffoldDefaults.snackbarBottomSpacing

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        data class DemoTap(
            val center: Offset,
            val createdAtMs: Long,
        )

        data class DemoReleaseDot(
            val center: Offset,
            val createdAtMs: Long,
        )
        val taps = remember { mutableStateListOf<DemoTap>() }
        val releaseDots = remember { mutableStateListOf<DemoReleaseDot>() }
        val nowMsState = remember { mutableStateOf(0L) }
        val activeTouchCenter = remember { mutableStateOf<Offset?>(null) }
        val pointerDownCenter = remember { mutableStateOf<Offset?>(null) }
        val touchMovedBeyondClick = remember { mutableStateOf(false) }
        val overlaySize = remember { mutableStateOf(IntSize.Zero) }
        val drawModifier =
            if (isDemoModeEnabled) {
                Modifier
                    .motionEventSpy { event ->
                        val point = Offset(event.x, event.y)
                        val inBounds =
                            point.x >= 0f &&
                                point.y >= 0f &&
                                point.x <= overlaySize.value.width.toFloat() &&
                                point.y <= overlaySize.value.height.toFloat()
                        when (event.actionMasked) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                activeTouchCenter.value = point
                                pointerDownCenter.value = point
                                touchMovedBeyondClick.value = false
                            }
                            android.view.MotionEvent.ACTION_MOVE -> {
                                if (!inBounds) {
                                    activeTouchCenter.value = null
                                    touchMovedBeyondClick.value = true
                                } else {
                                    activeTouchCenter.value = point
                                    val downCenter = pointerDownCenter.value
                                    if (downCenter != null) {
                                        val dragDistance = hypot(point.x - downCenter.x, point.y - downCenter.y)
                                        if (dragDistance > DemoClickMoveThresholdPx) {
                                            touchMovedBeyondClick.value = true
                                        }
                                    }
                                }
                            }
                            android.view.MotionEvent.ACTION_UP -> {
                                val releasePoint = activeTouchCenter.value ?: point
                                if (!touchMovedBeyondClick.value && inBounds) {
                                    taps.add(DemoTap(releasePoint, System.currentTimeMillis()))
                                    releaseDots.add(DemoReleaseDot(releasePoint, System.currentTimeMillis()))
                                }
                                activeTouchCenter.value = null
                                pointerDownCenter.value = null
                                touchMovedBeyondClick.value = false
                            }
                            android.view.MotionEvent.ACTION_CANCEL -> {
                                activeTouchCenter.value = null
                                pointerDownCenter.value = null
                                touchMovedBeyondClick.value = false
                            }
                        }
                    }.drawWithContent {
                        drawContent()
                        val now = nowMsState.value
                        val keepDurationMs = 450L
                        taps.forEach { tap ->
                            val elapsed = now - tap.createdAtMs
                            val waveElapsed = elapsed - DemoWaveDelayMs
                            if (waveElapsed in 0 until keepDurationMs) {
                                val progress = waveElapsed.toFloat() / keepDurationMs.toFloat()
                                val radius = (18f + 24f * progress) * density
                                drawCircle(
                                    color = demoTouchStrokeColor.copy(alpha = (1f - progress) * 0.42f),
                                    radius = radius,
                                    center = tap.center,
                                    style = Stroke(width = 1.5f * density),
                                )
                            }
                        }
                        releaseDots.forEach { dot ->
                            val elapsed = now - dot.createdAtMs
                            if (elapsed < DemoReleaseDotDurationMs) {
                                val progress = elapsed.toFloat() / DemoReleaseDotDurationMs.toFloat()
                                val radius = (11f + 6f * progress) * density
                                val alphaCurve = 1f - progress.pow(1.8f)
                                drawCircle(
                                    color = demoTouchFillColor.copy(alpha = alphaCurve * 0.88f),
                                    radius = radius,
                                    center = dot.center,
                                )
                                drawCircle(
                                    color = demoTouchStrokeColor.copy(alpha = alphaCurve),
                                    radius = radius,
                                    center = dot.center,
                                    style = Stroke(width = 1.6f * density),
                                )
                            }
                        }
                        activeTouchCenter.value?.let { center ->
                            drawCircle(
                                color = demoTouchFillColor.copy(alpha = 0.88f),
                                radius = 11f * density,
                                center = center,
                            )
                            drawCircle(
                                color = demoTouchStrokeColor,
                                radius = 11f * density,
                                center = center,
                                style = Stroke(width = 1.6f * density),
                            )
                        }
                    }
            } else {
                Modifier
            }
        LaunchedEffect(isDemoModeEnabled) {
            if (!isDemoModeEnabled) {
                taps.clear()
                releaseDots.clear()
                activeTouchCenter.value = null
            }
        }
        LaunchedEffect(isDemoModeEnabled, taps.size, releaseDots.size) {
            if (!isDemoModeEnabled) {
                return@LaunchedEffect
            }
            while (taps.isNotEmpty() || releaseDots.isNotEmpty()) {
                val now = System.currentTimeMillis()
                nowMsState.value = now
                taps.removeAll { now - it.createdAtMs >= (DemoWaveDelayMs + 450L) }
                releaseDots.removeAll { now - it.createdAtMs >= DemoReleaseDotDurationMs }
                delay(16L)
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        overlaySize.value = coordinates.size
                    }.then(drawModifier),
        ) {
            content(
                PaddingValues(
                    top = safeTopPadding,
                    bottom = contentBottomPadding,
                ),
            )

            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                miniPlayer?.let { miniPlayerContent ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PlayerScaffoldDefaults.dockHorizontalPadding),
                    ) {
                        miniPlayerContent()
                    }
                    Spacer(modifier = Modifier.height(PlayerScaffoldDefaults.dockSectionSpacing))
                }

                bottomBar()
            }

            snackbarHost?.let { host ->
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                start = 16.dp,
                                top = 16.dp,
                                end = 16.dp,
                                bottom = snackbarBottomPadding,
                            ),
                ) {
                    host()
                }
            }
        }
    }
}

private const val DemoClickMoveThresholdPx = 10f
private const val DemoWaveDelayMs = 60L
private const val DemoReleaseDotDurationMs = 280L

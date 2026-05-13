package com.bag.audioandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import com.bag.audioandroid.ui.AudioAndroidApp
import com.bag.audioandroid.ui.EncodeProgressDebugScenario
import com.bag.audioandroid.ui.FlashDebugScenario
import com.bag.audioandroid.ui.MiniDebugScenario
import com.bag.audioandroid.ui.SavedAudioDebugScenario

class MainActivity : AppCompatActivity() {
    private val debugScenarioState = mutableStateOf<FlashDebugScenario?>(null)
    private val miniDebugScenarioState = mutableStateOf<MiniDebugScenario?>(null)
    private val encodeProgressDebugScenarioState = mutableStateOf<EncodeProgressDebugScenario?>(null)
    private val savedAudioDebugScenarioState = mutableStateOf<SavedAudioDebugScenario?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        debugScenarioState.value = FlashDebugScenario.fromIntent(intent)
        miniDebugScenarioState.value = MiniDebugScenario.fromIntent(intent)
        encodeProgressDebugScenarioState.value = EncodeProgressDebugScenario.fromIntent(intent)
        savedAudioDebugScenarioState.value = SavedAudioDebugScenario.fromIntent(intent)
        setContent {
            AudioAndroidApp(
                debugScenario = debugScenarioState.value,
                miniDebugScenario = miniDebugScenarioState.value,
                encodeProgressDebugScenario = encodeProgressDebugScenarioState.value,
                savedAudioDebugScenario = savedAudioDebugScenarioState.value,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        debugScenarioState.value = FlashDebugScenario.fromIntent(intent)
        miniDebugScenarioState.value = MiniDebugScenario.fromIntent(intent)
        encodeProgressDebugScenarioState.value = EncodeProgressDebugScenario.fromIntent(intent)
        savedAudioDebugScenarioState.value = SavedAudioDebugScenario.fromIntent(intent)
    }
}

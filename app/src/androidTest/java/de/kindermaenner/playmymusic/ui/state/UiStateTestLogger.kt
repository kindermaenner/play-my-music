package de.kindermaenner.playmymusic.ui.state

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag

private const val StateReachedPrefix = "STATE_REACHED: "

private val KnownStates = listOf(
    "main_screen",
    "settings_screen"
)

fun logState(stateId: String) {
    println("$StateReachedPrefix$stateId")
}

fun ComposeTestRule.logVisibleStates() {
    KnownStates.forEach { stateId ->
        val visible = runCatching {
            onNodeWithTag("state_$stateId", useUnmergedTree = true).fetchSemanticsNode()
        }.isSuccess

        if (visible) {
            logState(stateId)
        }
    }
}
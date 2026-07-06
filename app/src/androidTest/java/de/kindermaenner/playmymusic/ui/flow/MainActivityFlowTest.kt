package de.kindermaenner.playmymusic.ui.flow

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.rule.GrantPermissionRule
import de.kindermaenner.playmymusic.MainActivity
import de.kindermaenner.playmymusic.ui.state.logVisibleStates
import org.junit.Rule
import org.junit.Test

class MainActivityFlowTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainToSettingsAndBack_logsReachedStates() {
        composeTestRule.logVisibleStates()

        composeTestRule.onNodeWithTag("state_main_screen", useUnmergedTree = true)
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.logVisibleStates()

        composeTestRule.onNodeWithTag("state_settings_screen", useUnmergedTree = true)
            .assertExists()

        repeat(3) {
            composeTestRule.onNodeWithTag("settings_screen_scroll")
                .performTouchInput { swipeUp() }
        }

        composeTestRule.onNodeWithTag("back_button").performClick()
        composeTestRule.logVisibleStates()

        composeTestRule.onNodeWithTag("state_main_screen", useUnmergedTree = true)
            .assertExists()
    }
}
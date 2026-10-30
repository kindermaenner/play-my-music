package de.kindermaenner.playmymusic.ui.smoke

import android.Manifest
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.rule.GrantPermissionRule
import de.kindermaenner.playmymusic.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppSmokeTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun smokeTest_appLaunchesSuccessfully_andMainScreenIsDisplayed() {
        // Verify activity is created and in RESUMED lifecycle state
        val activity = composeTestRule.activity
        assertNotNull("MainActivity should be created", activity)
        assertEquals("MainActivity should reach RESUMED state", Lifecycle.State.RESUMED, activity.lifecycle.currentState)

        // Verify the main screen root state is present
        composeTestRule.onNodeWithTag("state_main_screen", useUnmergedTree = true)
            .assertExists()

        // Verify key navigation/action element exists
        composeTestRule.onNodeWithContentDescription("Settings")
            .assertExists()
    }
}

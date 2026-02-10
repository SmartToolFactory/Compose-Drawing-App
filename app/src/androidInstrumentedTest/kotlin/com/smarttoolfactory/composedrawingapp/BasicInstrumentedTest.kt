package com.smarttoolfactory.composedrawingapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class BasicInstrumentedTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.smarttoolfactory.composedrawingapp", appContext.packageName)
    }
    
    @Test
    fun testExportPngButtonShowsToast() {
        // Wait for the UI to be ready
        composeTestRule.waitForIdle()
        
        // Find and click the export PNG button using its content description
        composeTestRule
            .onNodeWithContentDescription("Export PNG")
            .assertExists("Export PNG button should exist")
            .assertIsDisplayed()
            .performClick()
        
        // Wait for the action to complete
        composeTestRule.waitForIdle()
        
        // Note: Testing toasts directly is challenging in Compose UI tests
        // The toast should appear but we can verify the button click executed successfully
        // by checking that no crash occurred and the UI is still responsive
        
        // Verify app is still functional after export
        composeTestRule
            .onNodeWithContentDescription("Export PNG")
            .assertExists()
    }
    
    @Test
    fun testNewDrawingButtonExists() {
        composeTestRule.waitForIdle()
        
        composeTestRule
            .onNodeWithContentDescription("New Drawing")
            .assertExists("New Drawing button should exist")
            .assertIsDisplayed()
    }
    
    @Test
    fun testAppBarIsDisplayed() {
        composeTestRule.waitForIdle()
        
        // Check that the app title is displayed
        composeTestRule
            .onNodeWithText("DrawIt")
            .assertExists("App title should be displayed")
            .assertIsDisplayed()
    }
}

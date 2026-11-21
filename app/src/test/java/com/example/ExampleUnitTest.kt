package com.example

import com.example.data.GameTurboSettings
import com.example.data.SystemHardwareController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure JVM unit tests for Turbo X deterministic domain & hardware policy helpers.
 */
class ExampleUnitTest {

    @Test
    fun `selectPackagesToTrim respects backgroundLimitIndex protected apps and own package`() {
        val known = listOf("com.game.a", "com.social.app", "com.music.app", "com.browser.app")
        val running = listOf("com.turbo.x", "com.chat.app", "com.social.app", "com.video.app")
        val protected = setOf("com.game.a")
        val own = "com.turbo.x"

        // Index 0 (Default) & Index 1 (No background): trims all distinct non-protected/non-own apps
        val trimDefault = SystemHardwareController.selectPackagesToTrim(
            allKnownPackages = known,
            runningAppPackages = running,
            protectedPackages = protected,
            ownPackage = own,
            backgroundLimitIndex = 0
        )
        assertEquals(
            listOf("com.chat.app", "com.social.app", "com.video.app", "com.music.app", "com.browser.app"),
            trimDefault
        )

        val trimZeroBg = SystemHardwareController.selectPackagesToTrim(
            allKnownPackages = known,
            runningAppPackages = running,
            protectedPackages = protected,
            ownPackage = own,
            backgroundLimitIndex = 1
        )
        assertEquals(trimDefault, trimZeroBg)

        // Index 2 (At most 1): retains 1 non-protected app
        val trimKeepOne = SystemHardwareController.selectPackagesToTrim(
            allKnownPackages = known,
            runningAppPackages = running,
            protectedPackages = protected,
            ownPackage = own,
            backgroundLimitIndex = 2
        )
        assertEquals(
            listOf("com.social.app", "com.video.app", "com.music.app", "com.browser.app"),
            trimKeepOne
        )

        // Index 3 (At most 2): retains 2 non-protected apps
        val trimKeepTwo = SystemHardwareController.selectPackagesToTrim(
            allKnownPackages = known,
            runningAppPackages = running,
            protectedPackages = protected,
            ownPackage = own,
            backgroundLimitIndex = 3
        )
        assertEquals(listOf("com.video.app", "com.music.app", "com.browser.app"), trimKeepTwo)

        // Index 4 (At most 3): retains 3 non-protected apps
        val trimKeepThree = SystemHardwareController.selectPackagesToTrim(
            allKnownPackages = known,
            runningAppPackages = running,
            protectedPackages = protected,
            ownPackage = own,
            backgroundLimitIndex = 4
        )
        assertEquals(listOf("com.music.app", "com.browser.app"), trimKeepThree)
    }

    @Test
    fun `evaluateForegroundGameTrigger filters disabled ownPackage nonGames and duplicates`() {
        val own = "com.turbo.x"
        val enabledSettings = GameTurboSettings(
            autoDetectGameLaunch = true,
            addedGamePackages = setOf("com.tencent.ig", "com.mojang.minecraftpe")
        )

        // Disabled -> always null
        assertNull(
            SystemHardwareController.evaluateForegroundGameTrigger(
                foregroundPkg = "com.tencent.ig",
                lastDetectedPkg = null,
                settings = enabledSettings.copy(autoDetectGameLaunch = false),
                ownPackage = own
            )
        )

        // Blank or own package -> null
        assertNull(
            SystemHardwareController.evaluateForegroundGameTrigger(
                foregroundPkg = "",
                lastDetectedPkg = null,
                settings = enabledSettings,
                ownPackage = own
            )
        )
        assertNull(
            SystemHardwareController.evaluateForegroundGameTrigger(
                foregroundPkg = own,
                lastDetectedPkg = null,
                settings = enabledSettings,
                ownPackage = own
            )
        )

        // Non-game foreground app -> null
        assertNull(
            SystemHardwareController.evaluateForegroundGameTrigger(
                foregroundPkg = "com.android.settings",
                lastDetectedPkg = null,
                settings = enabledSettings,
                ownPackage = own
            )
        )

        // Already detected in current session -> debounced to null
        assertNull(
            SystemHardwareController.evaluateForegroundGameTrigger(
                foregroundPkg = "com.tencent.ig",
                lastDetectedPkg = "com.tencent.ig",
                settings = enabledSettings,
                ownPackage = own
            )
        )

        // Newly opened added game -> returns package
        assertEquals(
            "com.mojang.minecraftpe",
            SystemHardwareController.evaluateForegroundGameTrigger(
                foregroundPkg = "com.mojang.minecraftpe",
                lastDetectedPkg = "com.tencent.ig",
                settings = enabledSettings,
                ownPackage = own
            )
        )
    }
}

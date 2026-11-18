package com.example.time.blocking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ActionCooldown:
 * Validates cooldown throttling, duplicate event suppression, and loop safeguards.
 */
class ActionCooldownTest {

    private lateinit var cooldown: ActionCooldown

    @Before
    fun setUp() {
        cooldown = ActionCooldown(cooldownMillis = 1000L, maxConsecutiveAttempts = 2)
    }

    @Test
    fun testInitialActionAllowed() {
        assertTrue(cooldown.canExecuteAction("BACK", currentTime = 10000L))
    }

    @Test
    fun testActionBlockedDuringCooldown() {
        cooldown.recordAction("BACK", currentTime = 10000L)

        // Only 500ms later (less than 1000ms cooldown)
        assertFalse(cooldown.canExecuteAction("BACK", currentTime = 10500L))

        // Different action still blocked by global cooldown
        assertFalse(cooldown.canExecuteAction("NAVIGATE_INBOX", currentTime = 10800L))
    }

    @Test
    fun testActionAllowedAfterCooldownExpires() {
        cooldown.recordAction("BACK", currentTime = 10000L)

        // 1100ms later (greater than 1000ms cooldown)
        assertTrue(cooldown.canExecuteAction("BACK", currentTime = 11100L))
    }

    @Test
    fun testMaxConsecutiveAttemptsBounded() {
        // First attempt
        assertTrue(cooldown.canExecuteAction("BACK", currentTime = 10000L))
        cooldown.recordAction("BACK", currentTime = 10000L)
        assertEquals(1, cooldown.getConsecutiveAttempts())

        // Second attempt after cooldown
        assertTrue(cooldown.canExecuteAction("BACK", currentTime = 11500L))
        cooldown.recordAction("BACK", currentTime = 11500L)
        assertEquals(2, cooldown.getConsecutiveAttempts())

        // Third attempt exceeds maxConsecutiveAttempts = 2
        assertFalse(cooldown.canExecuteAction("BACK", currentTime = 13000L))
    }

    @Test
    fun testResetClearsCooldown() {
        cooldown.recordAction("BACK", currentTime = 10000L)
        assertFalse(cooldown.canExecuteAction("BACK", currentTime = 10200L))

        cooldown.reset()
        assertTrue(cooldown.canExecuteAction("BACK", currentTime = 10200L))
        assertEquals(0, cooldown.getConsecutiveAttempts())
    }
}


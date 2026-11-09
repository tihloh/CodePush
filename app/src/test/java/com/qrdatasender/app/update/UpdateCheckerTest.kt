package com.qrdatasender.app.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test fun newerStableVersion() {
        assertTrue(UpdateChecker.isNewer("1.6.2", "1.6.1"))
        assertTrue(UpdateChecker.isNewer("v2.0.0", "1.9.9"))
        assertTrue(UpdateChecker.isNewer("1.6.1.1", "1.6.1"))
    }

    @Test fun installedVersionNotOlder() {
        assertFalse(UpdateChecker.isNewer("1.6.1", "1.6.1"))
        assertFalse(UpdateChecker.isNewer("1.5.9", "1.6.1"))
        assertFalse(UpdateChecker.isNewer("bad-version", "1.6.1"))
        assertFalse(UpdateChecker.isNewer("1.6.2-rc1", "1.6.1"))
    }
}

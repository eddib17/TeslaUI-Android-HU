package io.github.eddib17.actyhu

import org.junit.Assert.assertEquals
import org.junit.Test

// Proves the unit-test pipeline runs in CI. Real tests (protocol parser etc.) arrive with P0-3.
class BuildSmokeTest {
    @Test
    fun unitTestsRun() {
        assertEquals("io.github.eddib17.actyhu", BuildSmokeTest::class.java.packageName)
    }
}

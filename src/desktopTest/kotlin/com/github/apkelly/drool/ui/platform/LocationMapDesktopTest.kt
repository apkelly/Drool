package com.github.apkelly.drool.ui.platform

import kotlin.test.Test
import kotlin.test.assertContains

class LocationMapDesktopTest {
    @Test
    fun staticMapUrlUsesCoordinatesAndMarker() {
        val url = staticMapUrl(
            latitude = -33.8688,
            longitude = 151.2093,
            address = "Ignored address",
            apiKey = "test-key",
        )

        assertContains(url, "center=-33.8688%2C151.2093")
        assertContains(url, "markers=color%3Ared%7C-33.8688%2C151.2093")
        assertContains(url, "key=test-key")
    }

    @Test
    fun staticMapUrlEncodesAddressWhenCoordinatesAreUnavailable() {
        val url = staticMapUrl(
            latitude = null,
            longitude = null,
            address = "1 Main Street, Sydney",
            apiKey = "test-key",
        )

        assertContains(url, "center=1+Main+Street%2C+Sydney")
        assertContains(url, "markers=color%3Ared%7C1+Main+Street%2C+Sydney")
    }
}

package com.github.apkelly.drool.logging

import co.touchlab.kermit.Logger

object DroolLog {
    fun withTag(tag: String): Logger = Logger.withTag(tag)
}

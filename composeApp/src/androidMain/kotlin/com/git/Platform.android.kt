package com.git

import android.os.Build
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    @OptIn(ExperimentalUuidApi::class)
    override fun uuid(): String {
        return Uuid.toString()
    }

    @OptIn(ExperimentalTime::class)
    override fun currentTimeMillis(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }

    override fun format(format: String, vararg args: Any?): String {
        return String.format(format, args)
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()

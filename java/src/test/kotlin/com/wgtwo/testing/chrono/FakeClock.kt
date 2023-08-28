package com.wgtwo.testing.chrono

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class FakeClock(
    private var instant: Instant,
    private var zoneId: ZoneId = ZoneOffset.UTC,
) : Clock() {
    override fun getZone() = zoneId
    override fun withZone(zone: ZoneId) = this.apply { zoneId = zone }
    override fun instant() = instant

    @Synchronized
    fun advance(duration: Duration) {
        instant += duration
    }

    operator fun plusAssign(duration: Duration) = advance(duration)
}

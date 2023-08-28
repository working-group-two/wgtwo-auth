package com.wgtwo.testing.chrono

import java.time.Duration

val Int.minutes: Duration
    get() = Duration.ofMinutes(this.toLong())

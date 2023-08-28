package com.wgtwo.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class FakeClock extends Clock {
  private Instant instant;
  private ZoneId zoneId = ZoneOffset.UTC;

  public FakeClock(Instant instant) {
    this.instant = instant;
  }

  public static FakeClock forInstant(Instant instant) {
    return new FakeClock(instant);
  }

  @Override
  public ZoneId getZone() {
    return zoneId;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    this.zoneId = zone;
    return this;
  }

  @Override
  public Instant instant() {
    return instant;
  }

  public synchronized void tick(Duration duration) {
    instant = instant.plus(duration);
  }
}

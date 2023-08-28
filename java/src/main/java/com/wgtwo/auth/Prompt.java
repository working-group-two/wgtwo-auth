package com.wgtwo.auth;

import org.jetbrains.annotations.NotNull;

public enum Prompt {
  /** Show login, even if there is an existing session */
  LOGIN("login"),
  /** Show consent screen, even if there is an existing consent */
  CONSENT("consent"),
  /** Show login and consent screen, even if there is an existing session and consent */
  LOGIN_AND_CONSENT("login consent"),
  /**
   * Do not show login or consent.
   *
   * <p>If no session and consent exists, fail the request.
   */
  NONE("none"),
  /**
   * Default setting.
   *
   * <p>Shows login and consent depending on session and consent available.
   */
  DEFAULT(""),
  ;

  @NotNull public final String value;

  Prompt(@NotNull String value) {
    this.value = value;
  }
}

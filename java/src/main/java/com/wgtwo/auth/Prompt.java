package com.wgtwo.auth;

public enum Prompt {
    /**
     * Show login, even if there is an existing session
     */
    LOGIN("login"),
    /**
     * Show consent screen, even if there is an existing consent
     */
    CONSENT("consent"),
    /**
     * Show login and consent screen, even if there is an existing session and consent
     */
    LOGIN_AND_CONSENT("login consent"),
    /**
     * Do not show login or consent.
     *
     * If no session and consent exists, fail the request.
     */
    NONE("none"),
    /**
     * Default setting.
     *
     * Shows login and consent depending on session and consent available.
     */
    DEFAULT(""),
    ;

    public final String value;

    Prompt(String value) {
        this.value = value;
    }
}

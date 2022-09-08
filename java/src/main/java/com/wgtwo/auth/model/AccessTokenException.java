package com.wgtwo.auth.model;

import java.util.Objects;

public class AccessTokenException extends RuntimeException {
    private final String error;
    private final String description;

    public AccessTokenException(String error, String description) {
        super("Could not get token: " + error + " => " + description);
        this.error = error;
        this.description = description;
    }

    public String getError() {
        return error;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessTokenException that = (AccessTokenException) o;
        return Objects.equals(error, that.error) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(error, description);
    }
}

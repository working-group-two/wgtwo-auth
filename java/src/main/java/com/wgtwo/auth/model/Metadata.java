package com.wgtwo.auth.model;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Metadata {
  private final String openIdToken;
  private DecodedJWT jwt = null;

  public Metadata(String openIdToken) {
    this.openIdToken = openIdToken;
  }

  @NotNull
  public String getOpenIdToken() {
    return openIdToken;
  }

  @NotNull
  public DecodedJWT getJwt() {
    if (jwt == null) {
      jwt = JWT.decode(openIdToken);
    }
    return jwt;
  }

  @Nullable
  public String getPhone() {
    return getJwt().getClaim("phone_number").asString();
  }

  @Nullable
  public String getNonce() {
    return getJwt().getClaim("nonce").asString();
  }
}

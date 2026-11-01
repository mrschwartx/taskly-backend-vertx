package com.taskly.backend.users.presentation;

import io.vertx.core.http.HttpServerRequest;
import java.time.Duration;

public class AuthCookie {

  private final String name;

  private final Duration maxAge;

  private final String domain;

  private final boolean production;

  public AuthCookie(String name, Duration maxAge, String domain, boolean production) {
    this.name = name;
    this.maxAge = maxAge;
    this.domain = domain;
    this.production = production;
  }

  public String name() {
    return name;
  }

  public String issue(String token) {
    return build(token, maxAge);
  }

  public String clear() {
    return build("", Duration.ZERO);
  }

  public String read(HttpServerRequest request) {
    for (String header : request.headers().getAll("Cookie")) {
      for (String pair : header.split(";")) {
        int separator = pair.indexOf('=');
        if (separator > 0 && name.equals(pair.substring(0, separator).strip())) {
          return unquote(pair.substring(separator + 1).strip());
        }
      }
    }
    return null;
  }

  private static String unquote(String value) {
    if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }

  private String build(String value, Duration age) {
    StringBuilder cookie = new StringBuilder(name).append('=').append(value)
      .append("; Path=/")
      .append("; Max-Age=").append(age.toSeconds())
      .append("; HttpOnly");
    if (production) {
      if (domain != null && !domain.isBlank()) {
        cookie.append("; Domain=").append(domain);
      }
      cookie.append("; Secure; SameSite=None");
    }
    else {
      cookie.append("; SameSite=Lax");
    }
    return cookie.toString();
  }
}

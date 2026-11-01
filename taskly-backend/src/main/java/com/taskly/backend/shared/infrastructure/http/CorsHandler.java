package com.taskly.backend.shared.infrastructure.http;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

public class CorsHandler implements Handler<RoutingContext> {

  private static final String METHODS = "GET,POST,PUT,DELETE,OPTIONS,PATCH";

  private static final String HEADERS = "Content-Type,Authorization";

  private static final String MAX_AGE = "1800";

  private final String allowedOrigin;

  public CorsHandler(String allowedOrigin) {
    this.allowedOrigin = allowedOrigin.endsWith("/")
      ? allowedOrigin.substring(0, allowedOrigin.length() - 1)
      : allowedOrigin;
  }

  @Override
  public void handle(RoutingContext context) {
    String origin = context.request().getHeader("Origin");
    if (origin == null) {
      context.next();
      return;
    }
    context.response().putHeader("Vary", "Origin");
    if (!allowedOrigin.equals(origin)) {
      context.next();
      return;
    }
    context.response()
      .putHeader("Access-Control-Allow-Origin", origin)
      .putHeader("Access-Control-Allow-Credentials", "true");
    boolean preflight = context.request().method() == HttpMethod.OPTIONS
      && context.request().getHeader("Access-Control-Request-Method") != null;
    if (preflight) {
      context.response()
        .putHeader("Access-Control-Allow-Methods", METHODS)
        .putHeader("Access-Control-Allow-Headers", HEADERS)
        .putHeader("Access-Control-Max-Age", MAX_AGE)
        .setStatusCode(200)
        .end();
      return;
    }
    context.next();
  }
}

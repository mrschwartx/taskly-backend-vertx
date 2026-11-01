package com.taskly.backend.shared.infrastructure.http;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class SecurityHeadersHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {
    context.response()
      .putHeader("X-Content-Type-Options", "nosniff")
      .putHeader("X-Frame-Options", "DENY");
    context.next();
  }
}

package com.taskly.backend.shared.infrastructure.http;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestLoggingHandler implements Handler<RoutingContext> {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingHandler.class);

  @Override
  public void handle(RoutingContext context) {
    long startedAt = System.nanoTime();
    String method = context.request().method().name();
    String path = context.request().path();
    context.response().endHandler(done -> {
      double durationMs = Math.round((System.nanoTime() - startedAt) / 10_000.0) / 100.0;
      log.info("HTTP request method={} path={} status={} durationMs={}", method, path,
        context.response().getStatusCode(), durationMs);
    });
    context.next();
  }
}

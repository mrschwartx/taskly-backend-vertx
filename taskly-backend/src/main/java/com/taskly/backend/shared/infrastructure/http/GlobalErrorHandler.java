package com.taskly.backend.shared.infrastructure.http;

import com.taskly.backend.shared.domain.DomainException;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalErrorHandler implements Handler<RoutingContext> {

  private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

  private static final String DEFAULT_MESSAGE = "We're having technical issues. Please try again later.";

  private static final Map<String, Integer> STATUS_BY_CODE = Map.of(
    "VALIDATION_ERROR", 400,
    "NOT_FOUND", 404,
    "ALREADY_EXISTS", 422,
    "AUTHENTICATION_FAILED", 401,
    "FORBIDDEN", 403);

  private final long avatarMaxMegabytes;

  public GlobalErrorHandler(long avatarMaxMegabytes) {
    this.avatarMaxMegabytes = avatarMaxMegabytes;
  }

  @Override
  public void handle(RoutingContext context) {
    if (context.response().ended()) {
      return;
    }
    Throwable failure = context.failure();
    int status = context.statusCode();
    if (failure instanceof DomainException error) {
      reject(context, STATUS_BY_CODE.getOrDefault(error.getCode(), 500), error.getMessage());
    }
    else if (failure instanceof HttpFailure error) {
      reject(context, error.status(), error.getMessage());
    }
    else if (status == 413) {
      reject(context, 400, "File size exceeds " + avatarMaxMegabytes + "MB");
    }
    else if (status == 404) {
      HttpResponses.message(context, 404, "Not found");
    }
    else if (status >= 400 && status < 500) {
      String message = failure != null && failure.getMessage() != null ? failure.getMessage() : "Bad request";
      reject(context, status, message);
    }
    else {
      log.error("Unhandled error: {}", failure != null ? failure.getMessage() : status, failure);
      HttpResponses.error(context, 500, DEFAULT_MESSAGE);
    }
  }

  private void reject(RoutingContext context, int status, String message) {
    if (status >= 500) {
      log.error("Request failed status={} message={}", status, message);
      HttpResponses.error(context, status, DEFAULT_MESSAGE);
      return;
    }
    log.warn("Request rejected status={} message={}", status, message);
    HttpResponses.error(context, status, message);
  }
}

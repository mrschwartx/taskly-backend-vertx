package com.taskly.backend.shared.infrastructure.http;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.function.Function;

public final class HttpResponses {

  private static final String JSON = "application/json";

  private HttpResponses() {
  }

  public static void json(RoutingContext context, int status, String body) {
    context.response().setStatusCode(status).putHeader("Content-Type", JSON).end(body);
  }

  public static void message(RoutingContext context, int status, String message) {
    json(context, status, new JsonObject().put("message", message).encode());
  }

  public static void error(RoutingContext context, int status, String message) {
    json(context, status, new JsonObject().put("status", status).put("message", message).encode());
  }

  public static <T> void send(RoutingContext context, Future<T> result, Function<T, String> body) {
    result.onSuccess(value -> json(context, 200, body.apply(value))).onFailure(context::fail);
  }

  public static void withCookie(RoutingContext context, String cookie, String body) {
    context.response().putHeader("Set-Cookie", cookie);
    json(context, 200, body);
  }
}

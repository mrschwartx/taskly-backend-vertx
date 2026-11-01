package com.taskly.backend.shared.infrastructure.http;

import com.taskly.backend.shared.domain.ValidationException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public final class RequestBodies {

  private static final int UNSUPPORTED_MEDIA_TYPE = 415;

  private RequestBodies() {
  }

  public static Map<String, Object> read(RoutingContext context) {
    Buffer buffer = context.body() != null ? context.body().buffer() : null;
    if (buffer == null || buffer.length() == 0) {
      return Collections.emptyMap();
    }
    String contentType = context.request().getHeader("Content-Type");
    if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("json")) {
      throw new HttpFailure(UNSUPPORTED_MEDIA_TYPE, "Content-Type '" + contentType + "' is not supported.");
    }
    return parse(buffer);
  }

  static Map<String, Object> parse(Buffer buffer) {
    Object decoded;
    try {
      decoded = Json.decodeValue(buffer);
    }
    catch (DecodeException error) {
      throw new ValidationException("Malformed JSON body");
    }
    if (decoded == null) {
      return Collections.emptyMap();
    }
    if (decoded instanceof JsonObject object) {
      return object.getMap();
    }
    throw new ValidationException("Malformed JSON body");
  }
}

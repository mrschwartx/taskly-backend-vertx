package com.taskly.backend.shared.infrastructure.http;

import io.vertx.ext.web.RoutingContext;
import java.util.List;

public final class QueryParameters {

  private QueryParameters() {
  }

  public static Object value(RoutingContext context, String key) {
    List<String> values = context.queryParam(key);
    if (values == null || values.isEmpty()) {
      return null;
    }
    return values.size() == 1 ? values.get(0) : List.copyOf(values);
  }
}

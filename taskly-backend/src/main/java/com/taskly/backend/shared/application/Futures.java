package com.taskly.backend.shared.application;

import io.vertx.core.Future;
import java.util.function.Supplier;

public final class Futures {

  private Futures() {
  }

  public static <T> Future<T> attempt(Supplier<Future<T>> body) {
    try {
      return body.get();
    }
    catch (RuntimeException error) {
      return Future.failedFuture(error);
    }
  }
}

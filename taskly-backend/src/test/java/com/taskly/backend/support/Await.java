package com.taskly.backend.support;

import io.vertx.core.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class Await {

  private static final long TIMEOUT_SECONDS = 30;

  private Await() {
  }

  public static <T> T await(Future<T> future) {
    try {
      return future.toCompletionStage().toCompletableFuture().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
    catch (ExecutionException error) {
      Throwable cause = error.getCause();
      if (cause instanceof RuntimeException runtime) {
        throw runtime;
      }
      if (cause instanceof Error failure) {
        throw failure;
      }
      throw new IllegalStateException(cause);
    }
    catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(error);
    }
    catch (TimeoutException error) {
      throw new IllegalStateException("Future did not complete in time", error);
    }
  }
}

package com.taskly.backend.shared.infrastructure.events;

import com.taskly.backend.shared.application.EventBus;
import com.taskly.backend.shared.domain.DomainEvent;
import io.vertx.core.Future;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryEventBus implements EventBus {

  private static final Logger log = LoggerFactory.getLogger(InMemoryEventBus.class);

  private final Map<String, List<Function<DomainEvent, Future<Void>>>> handlers = new ConcurrentHashMap<>();

  @Override
  public void subscribe(String eventName, Function<DomainEvent, Future<Void>> handler) {
    handlers.computeIfAbsent(eventName, key -> new CopyOnWriteArrayList<>()).add(handler);
  }

  @Override
  public Future<Void> publish(DomainEvent event) {
    Future<Void> chain = Future.succeededFuture();
    for (Function<DomainEvent, Future<Void>> handler : handlers.getOrDefault(event.name(), List.of())) {
      chain = chain.compose(done -> deliver(handler, event));
    }
    return chain;
  }

  private Future<Void> deliver(Function<DomainEvent, Future<Void>> handler, DomainEvent event) {
    try {
      return handler.apply(event).recover(error -> {
        logFailure(event, error);
        return Future.<Void>succeededFuture();
      });
    }
    catch (RuntimeException error) {
      logFailure(event, error);
      return Future.succeededFuture();
    }
  }

  private void logFailure(DomainEvent event, Throwable error) {
    log.error("Event handler failed for event {}: {}", event.name(), error.getMessage(), error);
  }
}

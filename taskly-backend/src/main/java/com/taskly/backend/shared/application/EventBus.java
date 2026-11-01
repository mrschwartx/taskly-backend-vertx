package com.taskly.backend.shared.application;

import com.taskly.backend.shared.domain.DomainEvent;
import io.vertx.core.Future;
import java.util.function.Function;

public interface EventBus {

  Future<Void> publish(DomainEvent event);

  void subscribe(String eventName, Function<DomainEvent, Future<Void>> handler);
}

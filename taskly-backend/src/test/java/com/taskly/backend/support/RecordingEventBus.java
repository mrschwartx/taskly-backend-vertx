package com.taskly.backend.support;

import com.taskly.backend.shared.application.EventBus;
import com.taskly.backend.shared.domain.DomainEvent;
import io.vertx.core.Future;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class RecordingEventBus implements EventBus {

  public final List<DomainEvent> published = new ArrayList<>();

  @Override
  public Future<Void> publish(DomainEvent event) {
    published.add(event);
    return Future.succeededFuture();
  }

  @Override
  public void subscribe(String eventName, Function<DomainEvent, Future<Void>> handler) {
  }
}

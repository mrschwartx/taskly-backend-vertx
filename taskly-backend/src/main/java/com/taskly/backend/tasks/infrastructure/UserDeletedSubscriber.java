package com.taskly.backend.tasks.infrastructure;

import com.taskly.backend.shared.application.EventBus;
import com.taskly.backend.shared.domain.IntegrationEvents;
import com.taskly.backend.tasks.application.DeleteTasksByOwner;

public class UserDeletedSubscriber {

  public UserDeletedSubscriber(EventBus eventBus, DeleteTasksByOwner deleteTasksByOwner) {
    eventBus.subscribe(IntegrationEvents.USER_DELETED,
      event -> deleteTasksByOwner.execute(String.valueOf(event.payload().get("userId"))).mapEmpty());
  }
}

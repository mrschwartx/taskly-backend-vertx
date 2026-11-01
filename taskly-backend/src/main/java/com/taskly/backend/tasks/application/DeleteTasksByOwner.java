package com.taskly.backend.tasks.application;

import com.taskly.backend.shared.application.Futures;
import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.tasks.domain.TaskRepository;
import io.vertx.core.Future;

public class DeleteTasksByOwner {

  private final TaskRepository taskRepository;

  public DeleteTasksByOwner(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  public Future<Integer> execute(Object ownerId) {
    return Futures.attempt(() -> taskRepository.deleteByOwner(EntityIds.parse(ownerId, "user")));
  }
}

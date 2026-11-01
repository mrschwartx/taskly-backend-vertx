package com.taskly.backend.tasks.application;

import com.taskly.backend.shared.application.Futures;
import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.shared.domain.ForbiddenException;
import com.taskly.backend.shared.domain.NotFoundException;
import com.taskly.backend.tasks.domain.Task;
import com.taskly.backend.tasks.domain.TaskRepository;
import io.vertx.core.Future;
import java.util.UUID;

public class GetTask {

  private final TaskRepository taskRepository;

  public GetTask(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  public Future<TaskDto> execute(UUID requesterId, String taskId) {
    return Futures.attempt(() -> {
      UUID id = EntityIds.parse(taskId, "task");
      return taskRepository.findById(id).map(found -> {
        Task task = found.orElseThrow(() -> new NotFoundException("Task not found!"));
        if (!task.isOwnedBy(requesterId)) {
          throw new ForbiddenException("You are not authorized to view this task");
        }
        return TaskDto.from(task);
      });
    });
  }
}

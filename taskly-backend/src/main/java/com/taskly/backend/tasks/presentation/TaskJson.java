package com.taskly.backend.tasks.presentation;

import com.taskly.backend.tasks.application.TaskDto;
import com.taskly.backend.tasks.application.TaskListDto;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public final class TaskJson {

  private TaskJson() {
  }

  public static JsonObject of(TaskDto task) {
    return new JsonObject()
      .put("_id", task.id())
      .put("name", task.name())
      .put("description", task.description())
      .put("priority", task.priority())
      .put("due", task.due())
      .put("status", task.status())
      .put("owner", task.owner())
      .put("createdAt", task.createdAt())
      .put("updatedAt", task.updatedAt());
  }

  public static JsonObject of(TaskListDto list) {
    JsonArray tasks = new JsonArray();
    list.tasks().forEach(task -> tasks.add(of(task)));
    return new JsonObject().put("tasks", tasks).put("taskCount", list.taskCount());
  }
}

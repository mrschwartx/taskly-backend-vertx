package com.taskly.backend.support;

import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.tasks.domain.Task;
import com.taskly.backend.tasks.domain.TaskPage;
import com.taskly.backend.tasks.domain.TaskRepository;
import io.vertx.core.Future;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryTaskRepository implements TaskRepository {

  private final Map<UUID, Task> tasks = new LinkedHashMap<>();

  @Override
  public UUID nextId() {
    return EntityIds.generate();
  }

  @Override
  public Future<Void> add(Task task) {
    tasks.put(task.getId(), task);
    return Future.succeededFuture();
  }

  @Override
  public Future<Boolean> update(Task task) {
    if (!tasks.containsKey(task.getId())) {
      return Future.succeededFuture(false);
    }
    tasks.put(task.getId(), task);
    return Future.succeededFuture(true);
  }

  @Override
  public Future<Optional<Task>> findById(UUID id) {
    return Future.succeededFuture(Optional.ofNullable(tasks.get(id)));
  }

  @Override
  public Future<TaskPage> findByOwner(UUID ownerId, String status, String orderBy, int page, int pageSize) {
    List<Task> owned = new ArrayList<>(tasks.values().stream()
      .filter(task -> task.getOwnerId().equals(ownerId))
      .filter(task -> status == null || task.getStatus().value().equals(status))
      .toList());
    if ("name".equals(orderBy)) {
      owned.sort(Comparator.comparing(task -> task.getName().value()));
    }
    int from = Math.min((page - 1) * pageSize, owned.size());
    int to = Math.min(from + pageSize, owned.size());
    return Future.succeededFuture(new TaskPage(owned.subList(from, to), owned.size()));
  }

  @Override
  public Future<Boolean> deleteById(UUID id) {
    return Future.succeededFuture(tasks.remove(id) != null);
  }

  @Override
  public Future<Integer> deleteByOwner(UUID ownerId) {
    int before = tasks.size();
    tasks.values().removeIf(task -> task.getOwnerId().equals(ownerId));
    return Future.succeededFuture(before - tasks.size());
  }
}

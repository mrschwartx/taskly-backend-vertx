package com.taskly.backend.tasks.domain;

import io.vertx.core.Future;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {

  UUID nextId();

  Future<Void> add(Task task);

  Future<Boolean> update(Task task);

  Future<Optional<Task>> findById(UUID id);

  Future<TaskPage> findByOwner(UUID ownerId, String status, String orderBy, int page, int pageSize);

  Future<Boolean> deleteById(UUID id);

  Future<Integer> deleteByOwner(UUID ownerId);
}

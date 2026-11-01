package com.taskly.backend.users.domain;

import io.vertx.core.Future;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

  UUID nextId();

  Future<Void> add(User user);

  Future<Boolean> update(User user);

  Future<Optional<User>> findById(UUID id);

  Future<Optional<User>> findByEmail(String email);

  Future<Boolean> existsByEmailOrUsername(String email, String username, UUID excludeId);

  Future<List<User>> search(String username, String email);

  Future<Boolean> deleteById(UUID id);
}

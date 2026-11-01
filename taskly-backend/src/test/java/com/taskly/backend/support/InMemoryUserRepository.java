package com.taskly.backend.support;

import com.taskly.backend.shared.domain.AlreadyExistsException;
import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.users.domain.User;
import com.taskly.backend.users.domain.UserRepository;
import io.vertx.core.Future;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryUserRepository implements UserRepository {

  private final Map<UUID, User> users = new LinkedHashMap<>();

  @Override
  public UUID nextId() {
    return EntityIds.generate();
  }

  @Override
  public Future<Void> add(User user) {
    if (matches(user.getEmail().value(), user.getUsername().value(), null)) {
      return Future.failedFuture(new AlreadyExistsException("Email or Username is already registered."));
    }
    users.put(user.getId(), user);
    return Future.succeededFuture();
  }

  @Override
  public Future<Boolean> update(User user) {
    if (!users.containsKey(user.getId())) {
      return Future.succeededFuture(false);
    }
    users.put(user.getId(), user);
    return Future.succeededFuture(true);
  }

  @Override
  public Future<Optional<User>> findById(UUID id) {
    return Future.succeededFuture(Optional.ofNullable(users.get(id)));
  }

  @Override
  public Future<Optional<User>> findByEmail(String email) {
    return Future.succeededFuture(
      users.values().stream().filter(user -> user.getEmail().value().equalsIgnoreCase(email)).findFirst());
  }

  @Override
  public Future<Boolean> existsByEmailOrUsername(String email, String username, UUID excludeId) {
    return Future.succeededFuture(matches(email, username, excludeId));
  }

  @Override
  public Future<List<User>> search(String username, String email) {
    List<User> result = new ArrayList<>();
    for (User user : users.values()) {
      boolean usernameMatches = username == null || username.isEmpty()
        || user.getUsername().value().toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT));
      boolean emailMatches = email == null || email.isEmpty()
        || user.getEmail().value().toLowerCase(Locale.ROOT).contains(email.toLowerCase(Locale.ROOT));
      if (usernameMatches && emailMatches) {
        result.add(user);
      }
    }
    return Future.succeededFuture(result);
  }

  @Override
  public Future<Boolean> deleteById(UUID id) {
    return Future.succeededFuture(users.remove(id) != null);
  }

  private boolean matches(String email, String username, UUID excludeId) {
    return users.values().stream()
      .filter(user -> !user.getId().equals(excludeId))
      .anyMatch(user -> (email != null && user.getEmail().value().equalsIgnoreCase(email))
        || (username != null && user.getUsername().value().equalsIgnoreCase(username)));
  }
}

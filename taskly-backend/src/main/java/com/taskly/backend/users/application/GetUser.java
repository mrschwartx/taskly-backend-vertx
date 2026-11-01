package com.taskly.backend.users.application;

import com.taskly.backend.shared.application.Futures;
import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.shared.domain.NotFoundException;
import com.taskly.backend.users.domain.UserRepository;
import io.vertx.core.Future;
import java.util.UUID;

public class GetUser {

  private final UserRepository userRepository;

  public GetUser(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public Future<UserDto> execute(String userId) {
    return Futures.attempt(() -> {
      UUID id = EntityIds.parse(userId, "user");
      return userRepository.findById(id).map(found -> found
        .map(UserDto::from)
        .orElseThrow(() -> new NotFoundException("User not found!")));
    });
  }
}

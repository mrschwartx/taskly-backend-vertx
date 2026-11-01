package com.taskly.backend.users.domain;

import io.vertx.core.Future;

public interface PasswordHasher {

  Future<String> hash(String plain);

  Future<Boolean> matches(String plain, String hash);
}

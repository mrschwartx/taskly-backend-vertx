package com.taskly.backend.support;

import com.taskly.backend.users.domain.PasswordHasher;
import io.vertx.core.Future;

public class FakePasswordHasher implements PasswordHasher {

  @Override
  public Future<String> hash(String plain) {
    return Future.succeededFuture("hashed:" + plain);
  }

  @Override
  public Future<Boolean> matches(String plain, String hash) {
    return Future.succeededFuture(("hashed:" + plain).equals(hash));
  }
}

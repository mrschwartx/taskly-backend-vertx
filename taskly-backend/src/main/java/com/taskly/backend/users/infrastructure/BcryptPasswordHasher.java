package com.taskly.backend.users.infrastructure;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.taskly.backend.users.domain.PasswordHasher;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.nio.charset.StandardCharsets;

public class BcryptPasswordHasher implements PasswordHasher {

  private static final int MAX_PASSWORD_BYTES = 72;

  private final Vertx vertx;

  private final int rounds;

  public BcryptPasswordHasher(Vertx vertx, int rounds) {
    this.vertx = vertx;
    this.rounds = rounds;
  }

  @Override
  public Future<String> hash(String plain) {
    return vertx.executeBlocking(
      () -> BCrypt.with(BCrypt.Version.VERSION_2B).hashToString(rounds, plain.toCharArray()), false);
  }

  @Override
  public Future<Boolean> matches(String plain, String hash) {
    return vertx.executeBlocking(() -> verify(plain, hash), false);
  }

  private static boolean verify(String plain, String hash) {
    if (plain.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
      return false;
    }
    try {
      return BCrypt.verifyer().verify(plain.toCharArray(), hash).verified;
    }
    catch (RuntimeException error) {
      return false;
    }
  }
}

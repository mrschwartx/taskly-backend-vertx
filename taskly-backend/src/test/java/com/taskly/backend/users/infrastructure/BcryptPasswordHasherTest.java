package com.taskly.backend.users.infrastructure;

import static com.taskly.backend.support.Await.await;
import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BcryptPasswordHasherTest {

  private Vertx vertx;

  private BcryptPasswordHasher hasher;

  @BeforeEach
  void setUp() {
    vertx = Vertx.vertx();
    hasher = new BcryptPasswordHasher(vertx, 4);
  }

  @AfterEach
  void tearDown() {
    await(vertx.close());
  }

  @Test
  void hashesWithSaltAndVerifies() {
    String first = await(hasher.hash("Password123!"));
    String second = await(hasher.hash("Password123!"));

    assertThat(first).startsWith("$2b$04$").isNotEqualTo(second);
    assertThat(await(hasher.matches("Password123!", first))).isTrue();
    assertThat(await(hasher.matches("wrong-password", first))).isFalse();
  }

  @Test
  void verifiesHashesProducedByNodeBcrypt() {
    String nodeHash = await(hasher.hash("password123")).replace("$2b$", "$2a$");

    assertThat(await(hasher.matches("password123", nodeHash))).isTrue();
  }

  @Test
  void treatsInvalidHashesAsMismatch() {
    assertThat(await(hasher.matches("password123", "not-a-bcrypt-hash"))).isFalse();
  }

  @Test
  void rejectsPasswordsLongerThanSeventyTwoBytes() {
    String hash = await(hasher.hash("Password123!"));

    assertThat(await(hasher.matches("é".repeat(37), hash))).isFalse();
  }
}

package com.taskly.backend.integration;

import com.taskly.backend.MainVerticle;
import com.taskly.backend.config.TasklyConfig;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class TestServer {

  static final TestServer INSTANCE = new TestServer();

  final Path publicDir;

  private final Vertx vertx;

  private final int port;

  private TestServer() {
    this.publicDir = createPublicDir();
    TestDatabase database = TestDatabase.INSTANCE;
    TasklyConfig config = TasklyConfig.from(Map.of(
      "APP_HOST", "127.0.0.1",
      "APP_PORT", "0",
      "DATABASE_URL", database.jdbcUrl,
      "DATABASE_USER", database.username,
      "DATABASE_PASSWORD", database.password,
      "AUTH_SECRET", "integration-test-secret-integration-test-secret",
      "BCRYPT_ROUNDS", "4",
      "PUBLIC_DIR", publicDir.toString()));
    this.vertx = Vertx.vertx();
    MainVerticle verticle = new MainVerticle(config);
    try {
      vertx.deployVerticle(verticle).toCompletionStage().toCompletableFuture().get(60, TimeUnit.SECONDS);
    }
    catch (Exception error) {
      throw new IllegalStateException("Test server failed to start: " + error.getMessage(), error);
    }
    this.port = verticle.actualPort();
    Runtime.getRuntime().addShutdownHook(new Thread(this::close));
  }

  int port() {
    return port;
  }

  private void close() {
    try {
      vertx.close().toCompletionStage().toCompletableFuture().get(15, TimeUnit.SECONDS);
    }
    catch (Exception error) {
      return;
    }
  }

  private static Path createPublicDir() {
    try {
      return Files.createTempDirectory("taskly-public");
    }
    catch (IOException error) {
      throw new UncheckedIOException(error);
    }
  }
}

package com.taskly.backend;

import com.taskly.backend.config.DotEnv;
import com.taskly.backend.config.LoggingBootstrap;
import com.taskly.backend.config.SeedRunner;
import com.taskly.backend.config.Services;
import com.taskly.backend.config.TasklyConfig;
import com.taskly.backend.shared.infrastructure.persistence.Migrations;
import com.taskly.backend.shared.infrastructure.persistence.PgPools;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TasklyBackendApplication {

  private static final String SEED_FLAG = "--seed";

  private static final long SHUTDOWN_TIMEOUT_SECONDS = 15;

  private TasklyBackendApplication() {
  }

  public static void main(String[] args) {
    Map<String, String> source = DotEnv.merged(Path.of(".env"), System.getenv());
    LoggingBootstrap.configure(source);

    TasklyConfig config;
    try {
      config = TasklyConfig.from(source);
    }
    catch (IllegalArgumentException error) {
      System.err.println(error.getMessage());
      System.exit(1);
      return;
    }

    Vertx vertx = Vertx.vertx();
    if (config.seed().enabled() || Arrays.asList(args).contains(SEED_FLAG)) {
      seed(vertx, config);
      return;
    }
    serve(vertx, config);
  }

  private static void serve(Vertx vertx, TasklyConfig config) {
    Logger log = LoggerFactory.getLogger(TasklyBackendApplication.class);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        vertx.close().toCompletionStage().toCompletableFuture().get(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      }
      catch (Exception error) {
        log.warn("Shutdown did not complete cleanly: {}", error.getMessage());
      }
    }));
    vertx.deployVerticle(new MainVerticle(config)).onFailure(error -> {
      log.error("Application failed to start: {}", error.getMessage(), error);
      System.exit(1);
    });
  }

  private static void seed(Vertx vertx, TasklyConfig config) {
    Migrations.run(vertx, config.database())
      .compose(done -> {
        Pool pool = PgPools.create(vertx, config.database());
        Services services = Services.create(vertx, pool, config);
        return new SeedRunner(config, services.registerUser, services.createTask).run()
          .eventually(() -> pool.close());
      })
      .onComplete(result -> {
        int exitCode = result.succeeded() ? result.result() : 1;
        if (result.failed()) {
          LoggerFactory.getLogger(TasklyBackendApplication.class)
            .error("Seeding failed: {}", result.cause().getMessage(), result.cause());
        }
        vertx.close().onComplete(closed -> System.exit(exitCode));
      });
  }
}

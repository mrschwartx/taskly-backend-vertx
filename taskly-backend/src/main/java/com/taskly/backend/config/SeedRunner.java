package com.taskly.backend.config;

import com.taskly.backend.shared.domain.AlreadyExistsException;
import com.taskly.backend.shared.domain.Timestamps;
import com.taskly.backend.tasks.application.CreateTask;
import com.taskly.backend.users.application.RegisterUser;
import io.vertx.core.Future;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeedRunner {

  private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

  private record SeedEntry(Map<String, Object> user, List<Map<String, Object>> tasks) {
  }

  private final TasklyConfig config;

  private final RegisterUser registerUser;

  private final CreateTask createTask;

  public SeedRunner(TasklyConfig config, RegisterUser registerUser, CreateTask createTask) {
    this.config = config;
    this.registerUser = registerUser;
    this.createTask = createTask;
  }

  public Future<Integer> run() {
    if (config.app().production() && !config.seed().allowProduction()) {
      log.error("Refusing to seed a production database");
      return Future.succeededFuture(1);
    }
    Future<Void> chain = Future.succeededFuture();
    for (SeedEntry entry : entries()) {
      chain = chain.compose(done -> seedEntry(entry));
    }
    return chain
      .map(done -> {
        log.info("Seeding completed");
        return 0;
      })
      .recover(error -> {
        log.error("Seeding failed: {}", error.getMessage(), error);
        return Future.succeededFuture(1);
      });
  }

  private Future<Void> seedEntry(SeedEntry entry) {
    String username = String.valueOf(entry.user().get("username"));
    return registerUser.execute(entry.user())
      .compose(result -> {
        UUID ownerId = UUID.fromString(result.user().id());
        Future<Void> tasks = Future.succeededFuture();
        for (Map<String, Object> task : entry.tasks()) {
          tasks = tasks.compose(done -> createTask.execute(ownerId, task).<Void>mapEmpty());
        }
        return tasks;
      })
      .<Void>map(done -> {
        log.info("Seeded user {}", username);
        return null;
      })
      .recover(error -> {
        if (error instanceof AlreadyExistsException) {
          log.warn("Skipped existing user {}", username);
          return Future.<Void>succeededFuture();
        }
        return Future.<Void>failedFuture(error);
      });
  }

  private static List<SeedEntry> entries() {
    String now = Timestamps.format(Timestamps.now());
    return List.of(
      new SeedEntry(
        Map.of("username", "johndoe123", "email", "johndoe123@mail.com", "password", "password123"),
        List.of(Map.of(
          "name", "Read Atomic Habits",
          "description", "Finish reading Atomic Habits by James Clear",
          "priority", "low",
          "due", now,
          "status", "todo"))),
      new SeedEntry(
        Map.of("username", "jane78", "email", "jane@mail.com", "password", "securepassword456"),
        List.of(Map.of(
          "name", "Learn MERN Stack",
          "description", "Learn the MERN stack and build a full-stack application with it",
          "priority", "high",
          "due", now,
          "status", "todo"))));
  }
}

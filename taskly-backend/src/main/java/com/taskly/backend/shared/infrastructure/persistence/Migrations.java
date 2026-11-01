package com.taskly.backend.shared.infrastructure.persistence;

import com.taskly.backend.config.TasklyConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.flywaydb.core.Flyway;

public final class Migrations {

  private Migrations() {
  }

  public static Future<Void> run(Vertx vertx, TasklyConfig.Database database) {
    return vertx.<Void>executeBlocking(() -> {
      Flyway.configure()
        .dataSource(database.jdbcUrl(), database.user(), database.password())
        .baselineOnMigrate(database.baselineOnMigrate())
        .baselineVersion(database.baselineVersion())
        .load()
        .migrate();
      return null;
    }, true);
  }
}

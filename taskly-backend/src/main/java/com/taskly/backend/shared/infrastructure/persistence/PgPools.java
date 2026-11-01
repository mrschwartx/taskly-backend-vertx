package com.taskly.backend.shared.infrastructure.persistence;

import com.taskly.backend.config.TasklyConfig;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public final class PgPools {

  private PgPools() {
  }

  public static Pool create(Vertx vertx, TasklyConfig.Database database) {
    PgConnectOptions connectOptions = PgConnectOptions.fromUri(database.vertxUri())
      .setUser(database.user())
      .setPassword(database.password())
      .setConnectTimeout(database.timeoutMillis());
    PoolOptions poolOptions = new PoolOptions().setMaxSize(database.poolMax());
    return PgBuilder.pool()
      .with(poolOptions)
      .connectingTo(connectOptions)
      .using(vertx)
      .build();
  }
}

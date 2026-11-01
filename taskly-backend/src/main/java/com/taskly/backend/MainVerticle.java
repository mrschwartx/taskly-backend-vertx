package com.taskly.backend;

import com.taskly.backend.config.HttpRouter;
import com.taskly.backend.config.Services;
import com.taskly.backend.config.TasklyConfig;
import com.taskly.backend.shared.infrastructure.persistence.Migrations;
import com.taskly.backend.shared.infrastructure.persistence.PgPools;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends VerticleBase {

  private static final Logger log = LoggerFactory.getLogger(MainVerticle.class);

  private final TasklyConfig config;

  private Pool pool;

  private HttpServer server;

  public MainVerticle(TasklyConfig config) {
    this.config = config;
  }

  public int actualPort() {
    return server.actualPort();
  }

  @Override
  public Future<?> start() {
    return Migrations.run(vertx, config.database())
      .compose(done -> {
        pool = PgPools.create(vertx, config.database());
        Services services = Services.create(vertx, pool, config);
        Router router = HttpRouter.create(vertx, pool, services, config);
        return vertx.createHttpServer()
          .requestHandler(router)
          .listen(config.server().port(), config.server().host());
      })
      .onSuccess(started -> {
        server = started;
        log.info("{} listening on {}:{}", config.server().name(), config.server().host(), started.actualPort());
      });
  }

  @Override
  public Future<?> stop() {
    return pool != null ? pool.close() : Future.succeededFuture();
  }
}
